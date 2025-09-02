def call(api, configCompleto, config, CONFIGURATION) {

    // Ruta MSBuild 2022 x64
    def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\amd64\\MSBuild.exe"

    // Ruta a los targets de WebApplications
    def vsToolsPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Microsoft\\VisualStudio\\v17.0\\WebApplications"

    stage("Restore ${api}") {
        dir("${env.REPO_PATH}") {
            bat """
                echo 📦 Restaurando paquetes NuGet...
                nuget restore "${env.REPO_PATH}\\ApiCrmVitalea.sln" -PackagesDirectory "${env.REPO_PATH}\\packages"
            """
        }
    }

    stage("Build SDK-style projects (.NET Standard)") {
        dir("${env.REPO_PATH}\\ViewModels") {
            bat """
                echo 🔧 Compilando ViewModels.csproj (.NET Standard)...
                dotnet restore ViewModels.csproj --verbosity normal
            """
        }
    }

    stage("Restore ${api}") {
        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            bat """
                echo 📦 Restaurando paquetes NuGet...
                nuget restore "ApiCrmVitalea.csproj" -PackagesDirectory "${env.REPO_PATH}\\packages"
            """
        }
    }

    stage("Deploy ${api} (.NET Framework 4.x)") {
        def apiConfig = [
            CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE]
        ]

        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                powershell """
                    Write-Host "📋 Leyendo perfil de publicación..."
                    [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                    \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }

                    if (-not \$profile) {
                        Write-Error "❌ No se encontró un perfil válido de MSDeploy"
                        exit 1
                    }

                    Write-Host "✅ Perfil encontrado: \$(\$profile.profileName)"
                    Write-Host "🔗 URL: \$(\$profile.publishUrl)"
                    Write-Host "🏗️ Sitio: \$(\$profile.msdeploySite)"

                    # Configurar MSBuildExtensionsPath para evitar errores de importación
                    \$env:MSBuildExtensionsPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild"

                    # Compilar y publicar la solución legacy
                    &   "${msbuildPath}" "ApiCrmVitalea.csproj" `
                        /p:DeployOnBuild=true `
                        /p:PublishProfile="\$profile.profileName" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:AllowUntrustedCertificate=true `
                        /p:BuildProjectReferences=true `
                        /p:TargetFrameworkVersion=v4.7.2 `
                        /p:VisualStudioVersion=15.0 `
                        /p:VSToolsPath="${vsToolsPath}" `
                        /p:ImportDirectoryBuildProps=true `
                        /p:ImportDirectoryBuildTargets=true `
                        /maxcpucount
                """
            }
        }
    }
}
