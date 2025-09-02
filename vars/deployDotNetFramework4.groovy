def call(api, configCompleto, config, CONFIGURATION) {

    // Ruta MSBuild 2022 x64
    def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\amd64\\MSBuild.exe"

    // Ruta padre de WebApplications
    def vsToolsPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Microsoft\\VisualStudio\\v17.0"

    // Ruta a los SDKs de .NET instalados
    def dotnetSdksPath = "C:\\Program Files\\dotnet\\sdk\\6.0.428\\Sdks"

    stage("Patch Directory.Build.props") {
        dir("${env.REPO_PATH}") {
            writeFile file: "Directory.Build.props", text: """
<Project>
  <PropertyGroup>
    <!-- Evita que MSBuild intente resolver SDKs inexistentes -->
    <ImportDirectoryBuildProps>false</ImportDirectoryBuildProps>
    <ImportDirectoryBuildTargets>false</ImportDirectoryBuildTargets>
  </PropertyGroup>
</Project>
"""
        }
    }

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
                dotnet build ViewModels.csproj -c Release
            """
        }
    }

    stage("Copy ViewModels DLL") {
        dir("${env.REPO_PATH}\\ViewModels\\bin\\Release\\netstandard2.0") {
            bat """
                echo 📂 Copiando ViewModels.dll a ApiCrmVitalea\\bin\\Release...
                if not exist "${env.REPO_PATH}\\ApiCrmVitalea\\bin\\Release" mkdir "${env.REPO_PATH}\\ApiCrmVitalea\\bin\\Release"
                copy ViewModels.dll "${env.REPO_PATH}\\ApiCrmVitalea\\bin\\Release\\" /Y
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

                    # Configurar rutas para que MSBuild encuentre los SDKs
                    \$env:MSBuildExtensionsPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild"
                    \$env:MSBuildSDKsPath = "${dotnetSdksPath}"

                    # Compilar y publicar la solución legacy
                    &   "${msbuildPath}" "ApiCrmVitalea.csproj" `
                        /p:DeployOnBuild=true `
                        /p:PublishProfile="\$profile.profileName" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:AllowUntrustedCertificate=true `
                        /p:BuildProjectReferences=false `
                        /p:TargetFrameworkVersion=v4.7.2 `
                        /p:VisualStudioVersion=15.0 `
                        /p:VSToolsPath="${vsToolsPath}" `
                        /maxcpucount
                """
            }
        }
    }

    stage("Cleanup Directory.Build.props") {
        dir("${env.REPO_PATH}") {
            bat """
                echo 🧹 Limpiando archivo temporal Directory.Build.props...
                if exist Directory.Build.props del /f /q Directory.Build.props
            """
        }
    }
}
