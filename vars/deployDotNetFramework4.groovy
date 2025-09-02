def call(api, configCompleto, config, CONFIGURATION) {

   def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\MSBuild.exe"


    // Restaurar paquetes NuGet solo para proyectos .NET Framework
    stage("Restore ${api} (.NET 4.x)") {
        dir("${env.REPO_PATH}/${api}") {
            bat """
                echo 📦 Restaurando paquetes NuGet para ${api}...
                dotnet build ViewModels.csproj -c ${CONFIGURATION} --no-restore
            """
        }
    }

    // Despliegue del proyecto .NET Framework usando MSBuild completo
    stage("Deploy ${api} (.NET 4.x)") {
        dir("${apiConfig.CS_PROJ_PATH}") {
            withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                powershell """
                    # ... tu código de lectura de publishProfile ...
                    
                    $projectFile = (Get-ChildItem -Filter "*.csproj" | Where-Object { $_ -notlike "*ViewModels*" }).FullName

                    Write-Host "🚀 Publicando: $projectFile"

                    & "${msbuildPath}" "$projectFile" `
                        /p:DeployOnBuild=true `
                        /p:PublishProfile="$profile.profileName" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:AllowUntrustedCertificate=true `
                        /p:BuildProjectReferences=false `
                        /maxcpucount
                """
            }
        }
    }

}
