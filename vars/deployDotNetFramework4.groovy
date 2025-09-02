def call(api, configCompleto, config, CONFIGURATION) {

    // Ruta MSBuild 2017
    def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\BuildTools\\MSBuild\\15.0\\Bin\\MSBuild.exe"

    // Restaurar paquetes NuGet a nivel de solución
    stage("Restore ${api} (.NET 4.x)") {
        dir("${env.REPO_PATH}") {
            bat """
                echo 📦 Restaurando paquetes NuGet para la solución...
                nuget restore "${env.REPO_PATH}\\ApiCrmVitalea.sln" -PackagesDirectory "${env.REPO_PATH}\\packages"
            """
        }
    }

    // Despliegue de la solución legacy usando MSBuild 2017
    stage("Deploy ${api} (.NET 4.x)") {
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

                    # Compilar y publicar toda la solución con MSBuild 2017
                    & "${msbuildPath}" "ApiCrmVitalea.sln" `
                        /p:DeployOnBuild=true `
                        /p:PublishProfile="\$profile.profileName" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:AllowUntrustedCertificate=true `
                        /p:BuildProjectReferences=true `
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
