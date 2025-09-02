def call(api, configCompleto, config, CONFIGURATION) {

    def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\MSBuild.exe"

    // Restaurar paquetes NuGet solo para proyectos .NET Framework
    stage("Restore ${api} (.NET 4.x)") {
        bat """
            echo 📦 Restaurando paquetes NuGet para ${api}...
            nuget restore ${api}.csproj -PackagesDirectory ..\\packages
        """
    }

    // Despliegue del proyecto .NET Framework con MSBuild (sin recompilar referencias SDK-style)
    stage("Deploy ${api} (.NET 4.x)") {
        def apiConfig = [
            CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
            CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
            URL: configCompleto.APIS[api].URL[config.AMBIENTE]
        ]

        dir("${apiConfig.CS_PROJ_PATH}") {
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

                    # Ejecutar MSBuild con parámetros de publicación
                    & "${msbuildPath}" "\$env:CS_PROJ_PATH" `
                        /p:DeployOnBuild=true `
                        /p:PublishProfile="\$profile.profileName" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:AllowUntrustedCertificate=true `
                        /p:VisualStudioVersion=17.0 `
                        /p:BuildProjectReferences=false
                """
            }
        }
    }
}
