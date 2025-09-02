def call(api, configCompleto, config, CONFIGURATION) {

    // --- Configuración de rutas necesarias ---
    def paths = [
        msbuild   : "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\amd64\\MSBuild.exe",
        vstools   : "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Microsoft\\VisualStudio\\v17.0",
        dotnetSdk : "C:\\Program Files\\dotnet\\sdk\\6.0.428\\Sdks"
    ]

    // --- Funciones auxiliares ---
    def backupCsproj = {
        bat """
            echo 📂 [Backup] Verificando archivo ApiCrmVitalea.csproj...
            if exist ApiCrmVitalea\\ApiCrmVitalea.csproj (
                echo 💾 [Backup] Creando respaldo de ApiCrmVitalea.csproj...
                copy ApiCrmVitalea\\ApiCrmVitalea.csproj ApiCrmVitalea\\ApiCrmVitalea.csproj.backup
                echo ✅ [Backup] Respaldo creado exitosamente.
            ) else (
                echo ⚠️ [Backup] No se encontró ApiCrmVitalea.csproj, se omite el respaldo.
            )
        """
    }

    def restoreCsproj = {
        bat """
            echo 🔄 [Restore] Restaurando ApiCrmVitalea.csproj original...
            if exist ApiCrmVitalea\\ApiCrmVitalea.csproj.backup (
                copy /Y ApiCrmVitalea\\ApiCrmVitalea.csproj.backup ApiCrmVitalea\\ApiCrmVitalea.csproj
                del ApiCrmVitalea\\ApiCrmVitalea.csproj.backup
                echo ✅ [Restore] Archivo restaurado exitosamente.
            ) else (
                echo ⚠️ [Restore] No se encontró respaldo, no se realizó la restauración.
            )
            if exist Directory.Build.props (
                echo 🧹 [Restore] Eliminando archivo temporal Directory.Build.props...
                del Directory.Build.props
            )
        """
    }

    def createBuildProps = {
        echo "⚙️ [Config] Creando archivo Directory.Build.props temporal..."
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

    def compileViewModels = {
        dir("ViewModels") {
            bat """
                echo 🔧 [Build] Compilando ViewModels.csproj (.NET Standard)...
                dotnet build ViewModels.csproj -c Release -p:MSBuildEnableWorkloadResolver=false
                if %ERRORLEVEL% neq 0 (
                    echo ❌ [Build] Error al compilar ViewModels.csproj
                    exit /b %ERRORLEVEL%
                )
                echo ✅ [Build] Compilación de ViewModels.csproj completada.
            """
        }
        bat """
            echo 📂 [Copy] Copiando ViewModels.dll al proyecto principal...
            if not exist "ApiCrmVitalea\\bin\\Release" mkdir "ApiCrmVitalea\\bin\\Release"
            copy "ViewModels\\bin\\Release\\netstandard2.0\\ViewModels.dll" "ApiCrmVitalea\\bin\\Release\\" /Y
            if %ERRORLEVEL% neq 0 (
                echo ❌ [Copy] Error al copiar ViewModels.dll
                exit /b %ERRORLEVEL%
            )
            echo ✅ [Copy] ViewModels.dll copiado correctamente.
        """
    }

    // --- Etapas del pipeline ---

    stage("Backup and Modify Project") {
        echo "🔒 [Stage] Iniciando respaldo y configuración inicial..."
        dir("${env.REPO_PATH}") {
            backupCsproj()
            createBuildProps()
        }
    }

    stage("Restore and Build") {
        echo "📦 [Stage] Restaurando paquetes NuGet y compilando ViewModels..."
        dir("${env.REPO_PATH}") {
            bat """
                echo 📦 [NuGet] Restaurando paquetes...
                nuget restore "${env.REPO_PATH}\\ApiCrmVitalea.sln" -PackagesDirectory "${env.REPO_PATH}\\packages" -DisableParallelProcessing
                if %ERRORLEVEL% neq 0 (
                    echo ❌ [NuGet] Error al restaurar paquetes.
                    exit /b %ERRORLEVEL%
                )
                echo ✅ [NuGet] Paquetes restaurados correctamente.
            """
            compileViewModels()
        }
    }

    stage("Modify Project References") {
        echo "📝 [Stage] Modificando referencias en ApiCrmVitalea.csproj..."
        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            powershell '''
                Write-Host "🔍 [Refs] Procesando referencias en ApiCrmVitalea.csproj..."
                $csprojPath = "ApiCrmVitalea.csproj"
                $xml = [xml](Get-Content $csprojPath)
                $ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
                $ns.AddNamespace("msbuild", "http://schemas.microsoft.com/developer/msbuild/2003")

                $projectReference = $xml.SelectSingleNode("//msbuild:ProjectReference[contains(@Include, 'ViewModels.csproj')]", $ns)
                if ($projectReference) {
                    $projectReference.ParentNode.RemoveChild($projectReference)
                    Write-Host "✅ [Refs] Referencia al proyecto ViewModels.csproj eliminada."
                } else {
                    Write-Host "⚠️ [Refs] No se encontró referencia a ViewModels.csproj."
                }

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
        echo "🚀 [Stage] Iniciando despliegue de la API ${api}..."
        def apiConfig = [
            CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE]
        ]

        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                powershell """
                    Write-Host "🔑 [Deploy] Cargando credenciales de publicación..."
                    [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                    \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }
                    
                    if (-not \$profile) { Write-Error "❌ [Deploy] Perfil MSDeploy no encontrado"; exit 1 }

                    Write-Host "⚙️ [Deploy] Configurando variables de entorno para MSBuild..."
                    \$env:MSBuildExtensionsPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild"
                    \$env:MSBuildSDKsPath = "${paths.dotnetSdk}"
                    \$env:VSToolsPath = "${paths.vstools}"
                    \$env:MSBuildEnableWorkloadResolver = "false"
                    
                    Write-Host "🚀 [Deploy] Ejecutando MSBuild con MSDeploy..."
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

                    if (\$LASTEXITCODE -ne 0) { Write-Error "❌ [Deploy] Error durante la publicación"; exit 1 }
                    Write-Host "✅ [Deploy] API publicada exitosamente en \$(\$profile.publishUrl)."
                """
            }
        }
    }

    stage("Cleanup") {
        echo "🧹 [Stage] Ejecutando limpieza final..."
        dir("${env.REPO_PATH}") {
            restoreCsproj()
        }
        echo "✅ [Stage] Limpieza completada."
    }
}
