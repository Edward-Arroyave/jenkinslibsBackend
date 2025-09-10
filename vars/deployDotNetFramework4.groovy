def call(api, configCompleto, config, CONFIGURATION) {

    // --- Configuración de rutas necesarias ---
    def paths = [
        msbuild   : "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\amd64\\MSBuild.exe",
        vstools   : "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Microsoft\\VisualStudio\\v17.0",
        dotnetSdk : "C:\\Program Files\\dotnet\\sdk\\6.0.428\\Sdks"
    ]

    // --- Etapas del pipeline ---
    stage("Backup and Modify Project") {
        echo "🔒 [Stage] Creando archivo Directory.Build.props..."
        dir("${env.REPO_PATH}") {
            writeFile file: "Directory.Build.props", text: """
            <Project>
            <PropertyGroup>
                <ImportDirectoryBuildProps>false</ImportDirectoryBuildProps>
                <ImportDirectoryBuildTargets>false</ImportDirectoryBuildTargets>
                <MSBuildEnableWorkloadResolver>false</MSBuildEnableWorkloadResolver>
            </PropertyGroup>
            </Project>
            """
            echo "✅ [Config] Archivo Directory.Build.props creado."
        }
    }

    stage("Restore NuGet Packages") {
        dir("${env.REPO_PATH}") {
            bat """
                echo 📦 [NuGet] Restaurando paquetes...
                nuget restore "${env.REPO_PATH}\\ApiCrmVitalea.sln" -PackagesDirectory "${env.REPO_PATH}\\packages" -DisableParallelProcessing
                if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
            """
        }
    }

    stage("Build ViewModels") {
        dir("${env.REPO_PATH}\\ViewModels") {
            bat """
                echo 🔧 [Build] Compilando ViewModels.csproj...
                dotnet build ViewModels.csproj -c Release -p:MSBuildEnableWorkloadResolver=false
                if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
            """
        }
    }

    stage("Copy ViewModels.dll") {
        dir("${env.REPO_PATH}") {
            bat """
                echo 📂 [Copy] Copiando ViewModels.dll...
                if not exist "ApiCrmVitalea\\bin\\Release" mkdir "ApiCrmVitalea\\bin\\Release"
                copy "ViewModels\\bin\\Release\\netstandard2.0\\ViewModels.dll" "ApiCrmVitalea\\bin\\Release\\" /Y
                if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
            """
        }
    }

    stage("Modify Project References") {
        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            powershell '''
                Write-Host "📝 [Refs] Procesando ApiCrmVitalea.csproj..."
                $csprojPath = "ApiCrmVitalea.csproj"
                $xml = [xml](Get-Content $csprojPath)
                $ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
                $ns.AddNamespace("msbuild", "http://schemas.microsoft.com/developer/msbuild/2003")

                $projectReference = $xml.SelectSingleNode("//msbuild:ProjectReference[contains(@Include, 'ViewModels.csproj')]", $ns)
                if ($projectReference) { $projectReference.ParentNode.RemoveChild($projectReference) }

                $itemGroup = $xml.CreateElement("ItemGroup", $xml.DocumentElement.NamespaceURI)
                $reference = $xml.CreateElement("Reference", $xml.DocumentElement.NamespaceURI)
                $reference.SetAttribute("Include", "ViewModels")

                $hintPath = $xml.CreateElement("HintPath", $xml.DocumentElement.NamespaceURI)
                $hintPath.InnerText = "bin\\Release\\ViewModels.dll"
                $reference.AppendChild($hintPath)
                $itemGroup.AppendChild($reference)
                $xml.Project.AppendChild($itemGroup)

                $xml.Save($csprojPath)
                Write-Host "✅ [Refs] Referencia a ViewModels.dll agregada correctamente."
            '''
        }
    }

 stage("Deploy ${api}") {
    def apiConfig = [
        CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE]
    ]

    echo "🔑 Usando credenciales ID: ${apiConfig.CREDENTIALS_ID}"

    dir("${env.REPO_PATH}\\ApiCrmVitalea") {
        withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
            powershell """
                Write-Host "🚀 [Deploy] Publicando API ${api}..."
                [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }

                if (!\$profile) {
                    Write-Error "❌ No se encontró perfil de publicación MSDeploy"
                    exit 1
                }

                Write-Host "📋 Información del perfil de publicación:"
                Write-Host " - URL: \$(\$profile.publishUrl)"
                Write-Host " - Sitio: \$(\$profile.msdeploySite)"
                Write-Host " - Usuario: \$(\$profile.userName)"

                # Crear archivo .pubxml temporal con formato correcto
                \$pubxmlContent = @"
<?xml version="1.0" encoding="utf-8"?>
<Project ToolsVersion="4.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
  <PropertyGroup>
    <WebPublishMethod>MSDeploy</WebPublishMethod>
    <PublishProvider>AzureWebSite</PublishProvider>
    <LastUsedBuildConfiguration>Release</LastUsedBuildConfiguration>
    <LastUsedPlatform>Any CPU</LastUsedPlatform>
    <SiteUrlToLaunchAfterPublish>https://\$(\$profile.msdeploySite)</SiteUrlToLaunchAfterPublish>
    <LaunchSiteAfterPublish>True</LaunchSiteAfterPublish>
    <ExcludeApp_Data>False</ExcludeApp_Data>
    <MSDeployServiceURL>https://\$(\$profile.publishUrl)/msdeploy.axd</MSDeployServiceURL>
    <DeployIisAppPath>\$(\$profile.msdeploySite)</DeployIisAppPath>
    <RemoteSitePhysicalPath />
    <SkipExtraFilesOnServer>True</SkipExtraFilesOnServer>
    <MSDeployPublishMethod>WMSVC</MSDeployPublishMethod>
    <EnableMSDeployBackup>True</EnableMSDeployBackup>
    <UserName>\$(\$profile.userName)</UserName>
    <Password>\$(\$profile.userPWD)</Password>
    <AllowUntrustedCertificate>true</AllowUntrustedCertificate>
  </PropertyGroup>
</Project>
"@

                # Usar una ruta absoluta válida en Windows
                \$pubxmlPath = "\$pwd\\AzurePubXml.pubxml"
                Set-Content -Path \$pubxmlPath -Value \$pubxmlContent

                Write-Host "📄 Archivo .pubxml temporal creado en: \$pubxmlPath"

                # Verificar que el archivo existe
                Write-Host "🔍 Verificando existencia del archivo .pubxml..."
                if (Test-Path \$pubxmlPath) {
                    Write-Host "✅ Archivo .pubxml encontrado en: \$pubxmlPath"
                    Write-Host "📄 Contenido del archivo:"
                    Get-Content \$pubxmlPath
                } else {
                    Write-Error "❌ Archivo .pubxml no encontrado en: \$pubxmlPath"
                    exit 1
                }

                # Ejecutar MSBuild con el archivo .pubxml
                & "${paths.msbuild}" "ApiCrmVitalea.csproj" `
                    /t:Build,Publish `
                    /p:Configuration=${CONFIGURATION} `
                    /p:DeployOnBuild=true `
                    /p:PublishProfile="\$pubxmlPath" `
                    /p:VisualStudioVersion=17.0 `
                    /p:VSToolsPath="${paths.vstools}" `
                    /p:BuildProjectReferences=false `
                    /p:SkipResolveProjectReferences=true `
                    /p:TargetFrameworkVersion=v4.7.2 `
                    /p:DeleteExistingFiles=True `
                    /maxcpucount `
                    /verbosity:minimal

                if (\$LASTEXITCODE -ne 0) { 
                    Write-Error "❌ Error en publicación"; 
                    exit 1 
                }

                Write-Host "✅ Publicación completada exitosamente"

                # Limpiar archivo temporal
                Remove-Item \$pubxmlPath -Force
            """
        }
    }
}
}
