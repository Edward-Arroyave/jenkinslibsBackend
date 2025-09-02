def call(api, configCompleto, config, CONFIGURATION) {

    // Ruta MSBuild 2022 x64
    def msbuildPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\amd64\\MSBuild.exe"

    // Ruta padre de WebApplications
    def vsToolsPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Microsoft\\VisualStudio\\v17.0"

    // Ruta a los SDKs de .NET instalados
    def dotnetSdksPath = "C:\\Program Files\\dotnet\\sdk\\6.0.428\\Sdks"

    stage("Backup ApiCrmVitalea.csproj") {
        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            bat """
                copy ApiCrmVitalea.csproj ApiCrmVitalea.csproj.backup
            """
        }
    }

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

    stage("Modify ApiCrmVitalea.csproj") {
        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            powershell '''
                $csprojPath = "ApiCrmVitalea.csproj"
                $xml = [xml](Get-Content $csprojPath)
                $ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
                $ns.AddNamespace("msbuild", "http://schemas.microsoft.com/developer/msbuild/2003")

                # Encontrar la referencia al proyecto ViewModels
                $projectReference = $xml.SelectSingleNode("//msbuild:ProjectReference[contains(@Include, 'ViewModels.csproj')]", $ns)
                if ($projectReference) {
                    # Remover la referencia al proyecto
                    $projectReference.ParentNode.RemoveChild($projectReference)

                    # Agregar referencia a la DLL
                    $itemGroup = $xml.CreateElement("ItemGroup", $xml.DocumentElement.NamespaceURI)
                    $reference = $xml.CreateElement("Reference", $xml.DocumentElement.NamespaceURI)
                    $reference.SetAttribute("Include", "ViewModels")
                    $hintPath = $xml.CreateElement("HintPath", $xml.DocumentElement.NamespaceURI)
                    $hintPath.InnerText = "bin\\Release\\ViewModels.dll"
                    $reference.AppendChild($hintPath)
                    $itemGroup.AppendChild($reference)
                    $xml.Project.AppendChild($itemGroup)

                    $xml.Save($csprojPath)
                    Write-Host "✅ Referencia al proyecto ViewModels reemplazada por referencia a DLL."
                } else {
                    Write-Host "⚠️ No se encontró la referencia al proyecto ViewModels. Puede que ya sea una referencia a DLL."
                }
            '''
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

                    # Configurar rutas críticas y deshabilitar el resolvedor de workloads
                    \$env:MSBuildExtensionsPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild"
                    \$env:MSBuildSDKsPath = "${dotnetSdksPath}"
                    \$env:VSToolsPath = "${vsToolsPath}"
                    \$env:MSBuildEnableWorkloadResolver = "false"

                    # Compilar y publicar el proyecto .NET Framework
                    &   "${msbuildPath}" "ApiCrmVitalea.csproj" `
                        /p:DeployOnBuild=true `
                        /p:PublishProfile="\$profile.profileName" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:AllowUntrustedCertificate=true `
                        /p:BuildProjectReferences=false `
                        /p:SkipResolveProjectReferences=true `
                        /p:TargetFrameworkVersion=v4.7.2 `
                        /p:VisualStudioVersion=17.0 `
                        /p:VSToolsPath="${vsToolsPath}" `
                        /maxcpucount
                """
            }
        }
    }

    stage("Cleanup") {
        dir("${env.REPO_PATH}") {
            bat """
                echo 🧹 Limpiando archivos temporales...
                if exist Directory.Build.props del /f /q Directory.Build.props
                if exist ApiCrmVitalea\\ApiCrmVitalea.csproj.backup (
                    copy /Y ApiCrmVitalea\\ApiCrmVitalea.csproj.backup ApiCrmVitalea\\ApiCrmVitalea.csproj
                    del /f /q ApiCrmVitalea\\ApiCrmVitalea.csproj.backup
                )
            """
        }
    }
}