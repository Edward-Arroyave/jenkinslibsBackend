def call(api, configCompleto, config, CONFIGURATION) {

   def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\MSBuild.exe"


    // Restaurar paquetes NuGet solo para proyectos .NET Framework
    stage("Restore ${api} (.NET 4.x)") {
        dir("${env.REPO_PATH}/${api}") {
            bat """
                echo 📦 Restaurando paquetes NuGet para ${api}...
                nuget restore ${api}.csproj -PackagesDirectory ..\\packages
            """
        }
    }

    // Despliegue del proyecto .NET Framework usando MSBuild completo
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

                    \$projectFile = (Get-ChildItem -Filter "*.csproj" | Where-Object { \$_ -notlike "*ViewModels*" }).FullName

                    Write-Host "🚀 Publicando: \$projectFile"

                    # ⚡ Compilar exactamente como Visual Studio, resolviendo referencias .NET Standard automáticamente
                    & "${msbuildPath}" "\$projectFile" `
                        /p:DeployOnBuild=true `
                        /p:PublishProfile="\$profile.profileName" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:AllowUntrustedCertificate=true `
                        /p:BuildProjectReferences=true `
                        /maxcpucount
                """
            }
        }
    }
}
