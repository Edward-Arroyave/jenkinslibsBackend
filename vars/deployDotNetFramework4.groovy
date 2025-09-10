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
        // Validar existencia de configuración para la API
        if (!configCompleto.APIS[api]) {
            error "❌ No se encontró configuración para la API: ${api}"
        }
        
        // Validar existencia de credenciales para el ambiente
        def credenciales = configCompleto.APIS[api].CREDENCIALES
        if (!credenciales) {
            error "❌ No se encontraron credenciales configuradas para la API: ${api}"
        }
        
        def credencialesId = credenciales[config.AMBIENTE]
        if (!credencialesId) {
            error "❌ No se encontraron credenciales para el ambiente ${config.AMBIENTE} en la API: ${api}"
        }
        
        echo "🔑 Usando credenciales ID: ${credencialesId}"
        
        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            withCredentials([file(credentialsId: credencialesId, variable: 'PUBLISH_SETTINGS')]) {
                powershell """
                    Write-Host "🚀 [Deploy] Publicando API ${api}..."
                    [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                    \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }
                    
                    # Validar perfil de publicación
                    if (!\$profile) {
                        Write-Error "❌ No se encontró perfil de publicación MSDeploy en el archivo de configuración"
                        exit 1
                    }
                    
                    Write-Host "📋 Información del perfil de publicación:"
                    Write-Host " - URL: \$(\$profile.publishUrl)"
                    Write-Host " - Sitio: \$(\$profile.msdeploySite)"
                    Write-Host " - Usuario: \$(\$profile.userName)"
                    
                    # Ejecutar MSBuild
                    & "${paths.msbuild}" "ApiCrmVitalea.csproj" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:DeployOnBuild=true `
                        /p:WebPublishMethod=MSDeploy `
                        /p:MsDeployServiceUrl="https://\$(\$profile.publishUrl)/msdeploy.axd" `
                        /p:DeployIisAppPath="\$(\$profile.msdeploySite)" `
                        /p:Username="\$(\$profile.userName)" `
                        /p:Password="\$(\$profile.userPWD)" `
                        /p:AllowUntrustedCertificate=true `
                        /p:VisualStudioVersion=17.0 `
                        /p:VSToolsPath="${paths.vstools}" `
                        /p:BuildProjectReferences=false `
                        /p:SkipResolveProjectReferences=true `
                        /p:TargetFrameworkVersion=v4.7.2 `
                        /maxcpucount `
                        /verbosity:minimal

                    if (\$LASTEXITCODE -ne 0) { 
                        Write-Error "❌ Error en publicación"; 
                        exit 1 
                    }
                """
            }
        }
    }
}
