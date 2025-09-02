def call(api, configCompleto, config, CONFIGURATION) {

    def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\MSBuild.exe"

    // 1️⃣ Restaurar paquetes NuGet
    stage("Restore ${api} (.NET 4.x & SDK)") {
        bat """
            echo 📦 Restaurando paquetes NuGet para ${api}...
            nuget restore ${api}.csproj -PackagesDirectory ..\\packages
        """
    }

    // 2️⃣ Compilar librerías SDK-style primero (ViewModels)
    stage("Build SDK-style projects") {
        dir("${env.REPO_PATH}/ViewModels") {
            bat """
                echo 🚀 Compilando librerías SDK-style con dotnet...
                dotnet build ViewModels.csproj -c ${CONFIGURATION} -o ..\\bin
            """
        }
    }

    // 3️⃣ Desplegar proyecto .NET Framework
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

                    \$projectFile = (Get-ChildItem -Filter "*.csproj" | Where-Object { \$_ -notlike "*ViewModels*" }).FullName
                    Write-Host "🚀 Publicando: \$projectFile"

                    # Compilar sin reconstruir ProjectReference, apuntando a la DLL de ViewModels
                    & "${msbuildPath}" "\$projectFile" `
                        /p:DeployOnBuild=true `
                        /p:WebPublishMethod=MSDeploy `
                        /p:MsDeployServiceUrl="\$url" `
                        /p:DeployIisAppPath="\$site" `
                        /p:UserName="\$user" `
                        /p:Password="\$pass" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:AllowUntrustedCertificate=true `
                        /p:VisualStudioVersion=17.0 `
                        /p:BuildProjectReferences=false `
                        /p:ReferencePath="${env.REPO_PATH}\\ViewModels\\bin"
                """
            }
        }
    }
}
