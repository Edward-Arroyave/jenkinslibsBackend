def call(api, configCompleto, config, CONFIGURATION) {

    // Path a MSBuild de Visual Studio (para proyectos .NET Framework clásicos)
    def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\MSBuild.exe"

    stage("Restore ${api} (.NET 4.x & SDK)") {
        // Restaurar paquetes NuGet
        bat """
            echo 📦 Restaurando paquetes NuGet para ${api}...
            nuget restore ${api}.csproj -PackagesDirectory ..\\packages
        """
    }

    stage("Build SDK-style projects (ViewModels)") {
        dir("${env.REPO_PATH}/ViewModels") {
            bat """
                echo 🚀 Compilando librerías SDK-style con dotnet...
                dotnet build ViewModels.csproj -c ${CONFIGURATION} -o ..\\bin
            """
        }
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

                    # Detectar si es SDK-style (netstandard) o .NET Framework clásico
                    \$projectFile = (Get-ChildItem -Filter "*.csproj" | Where-Object { \$_ -notlike "*ViewModels*" }).FullName
                    \$projContent = Get-Content \$projectFile
                    if (\$projContent -match '<Project Sdk="Microsoft.NET.Sdk">') {
                        Write-Host "🚀 Proyecto SDK-style detectado, usando dotnet msbuild"
                        dotnet msbuild "\$projectFile" /p:DeployOnBuild=true /p:WebPublishMethod=MSDeploy /p:MsDeployServiceUrl="\$url" /p:DeployIisAppPath="\$site" /p:UserName="\$user" /p:Password="\$pass" /p:Configuration=${CONFIGURATION} /p:AllowUntrustedCertificate=true /verbosity:minimal
                    } else {
                        Write-Host "🚀 Proyecto .NET Framework detectado, usando MSBuild clásico"
                        & "${msbuildPath}" "\$projectFile" /p:DeployOnBuild=true /p:WebPublishMethod=MSDeploy /p:MsDeployServiceUrl="\$url" /p:DeployIisAppPath="\$site" /p:UserName="\$user" /p:Password="\$pass" /p:Configuration=${CONFIGURATION} /p:AllowUntrustedCertificate=true /verbosity:minimal /p:VisualStudioVersion=17.0
                    }
                """
            }
        }
    }
}
