def call(api, configCompleto, config, CONFIGURATION) {

    def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\MSBuild.exe"
    def dotnetPath = "dotnet" // asumimos que dotnet está en PATH

    // 1️⃣ Restaurar paquetes NuGet solo para proyectos .NET Framework
    stage("Restore ${api} (.NET 4.x)") {
        bat """
            echo 📦 Restaurando paquetes NuGet para ${api}...
            nuget restore ${api}.csproj -PackagesDirectory ..\\packages
        """
    }

    // 2️⃣ Compilar proyectos SDK-style primero (ej: ViewModels)
    stage("Build SDK-style projects") {
        dir("${env.REPO_PATH}/ViewModels") {
            bat """
                echo 🚀 Compilando librerías SDK-style con dotnet...
                ${dotnetPath} build ViewModels.csproj -c ${CONFIGURATION} -o ..\\bin
            """
        }
    }

    // 3️⃣ Despliegue del proyecto .NET Framework sin recompilar referencias
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

                    # Buscar el .csproj de la API, ignorando librerías SDK-style
                    \$projectFile = (Get-ChildItem -Filter "*.csproj" | Where-Object { \$_ -notlike "*ViewModels*" }).FullName

                    Write-Host "🚀 Publicando: \$projectFile"

                    # Ejecutar MSBuild directamente, evitando que intente resolver SDK virtual
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
                        /p:RestorePackages=false
                """
            }
        }
    }
}
