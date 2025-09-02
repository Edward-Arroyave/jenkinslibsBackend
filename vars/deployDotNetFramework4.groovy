def call(api, configCompleto, config, CONFIGURATION) {

    // Ruta MSBuild 2017
    def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\BuildTools\\MSBuild\\15.0\\Bin\\MSBuild.exe"

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

    stage("Build .NET Framework projects") {
        dir("${env.REPO_PATH}") {
            bat """
                echo 🔧 Compilando proyecto principal (.NET Framework 4.7.2)...
                "${msbuildPath}" "ApiCrmVitalea\\ApiCrmVitalea.csproj" /p:Configuration=${CONFIGURATION} /p:TargetFrameworkVersion=v4.7.2 /maxcpucount
            """
        }
    }

    stage("Deploy ${api} (.NET Framework 4.x)") {
        def apiConfig = [
            CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE]
        ]

        dir("${env.REPO_PATH}") {
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

                    # Compilar y publicar la solución legacy
                    &   "${msbuildPath}" "ApiCrmVitalea\\ApiCrmVitalea.csproj" `
                        /p:DeployOnBuild=false `
                        /p:PublishProfile="\$profile.profileName" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:AllowUntrustedCertificate=false `
                        /p:BuildProjectReferences=false `
                        /p:TargetFrameworkVersion=v4.7.2 `
                        /p:VisualStudioVersion=15.0 `
                        /p:ImportDirectoryBuildProps=false `
                        /p:ImportDirectoryBuildTargets=false `
                        /maxcpucount
                """
            }
        }
    }
}