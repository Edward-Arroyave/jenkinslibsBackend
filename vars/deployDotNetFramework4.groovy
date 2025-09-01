def call(api, configCompleto, config, CONFIGURATION) {
    def msbuildPath = "C:\\Windows\\Microsoft.NET\\Framework\\v4.0.30319\\msbuild.exe"

    stage("Restore ${api} (.NET 4.x)") {
        bat """
            echo 📦 Restaurando paquetes NuGet para ${api}...
            nuget restore ${api}.csproj -PackagesDirectory ..\\packages
        """
    }

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

                    \$url = \$profile.publishUrl
                    \$site = \$profile.msdeploySite
                    \$user = \$profile.userName
                    \$pass = \$profile.userPWD

                    \$projectFile = (Get-ChildItem -Filter "*.csproj").FullName

                    Write-Host "🚀 Publicando: \$projectFile"

                    & "${msbuildPath}" "\$projectFile" /p:DeployOnBuild=true /p:WebPublishMethod=MSDeploy /p:MsDeployServiceUrl="\$url" /p:DeployIisAppPath="\$site" /p:UserName="\$user" /p:Password="\$pass" /p:Configuration=${CONFIGURATION} /p:AllowUntrustedCertificate=true /verbosity:minimal /p:VisualStudioVersion=16.0
                """
            }
        }
    }
}