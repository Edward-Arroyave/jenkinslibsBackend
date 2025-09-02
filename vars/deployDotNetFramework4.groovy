def call(api, configCompleto, config, CONFIGURATION) {

    // Configuración de rutas (centralizadas en un mapa para fácil mantenimiento)
    def paths = [
        msbuild   : "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Current\\Bin\\amd64\\MSBuild.exe",
        vstools   : "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild\\Microsoft\\VisualStudio\\v17.0",
        dotnetSdk : "C:\\Program Files\\dotnet\\sdk\\6.0.428\\Sdks"
    ]

    // --- Funciones auxiliares ---
    def backupCsproj = {
        bat """
            if exist ApiCrmVitalea\\ApiCrmVitalea.csproj (
                copy ApiCrmVitalea\\ApiCrmVitalea.csproj ApiCrmVitalea\\ApiCrmVitalea.csproj.backup
            )
        """
    }

    def restoreCsproj = {
        bat """
            if exist ApiCrmVitalea\\ApiCrmVitalea.csproj.backup (
                copy /Y ApiCrmVitalea\\ApiCrmVitalea.csproj.backup ApiCrmVitalea\\ApiCrmVitalea.csproj
                del ApiCrmVitalea\\ApiCrmVitalea.csproj.backup
            )
            if exist Directory.Build.props del Directory.Build.props
        """
    }

    def createBuildProps = {
        writeFile file: "Directory.Build.props", text: """
<Project>
  <PropertyGroup>
    <ImportDirectoryBuildProps>false</ImportDirectoryBuildProps>
    <ImportDirectoryBuildTargets>false</ImportDirectoryBuildTargets>
    <MSBuildEnableWorkloadResolver>false</MSBuildEnableWorkloadResolver>
  </PropertyGroup>
</Project>
"""
    }

    def compileViewModels = {
        dir("ViewModels") {
            bat """
                echo 🔧 Compilando ViewModels.csproj (.NET Standard)...
                dotnet build ViewModels.csproj -c Release -p:MSBuildEnableWorkloadResolver=false
            """
        }
        bat """
            echo 📂 Copiando ViewModels.dll...
            if not exist "ApiCrmVitalea\\bin\\Release" mkdir "ApiCrmVitalea\\bin\\Release"
            copy "ViewModels\\bin\\Release\\netstandard2.0\\ViewModels.dll" "ApiCrmVitalea\\bin\\Release\\" /Y
        """
    }

    // --- Stages ---
    stage("Backup and Modify Project") {
        dir("${env.REPO_PATH}") {
            backupCsproj()
            createBuildProps()
        }
    }

    stage("Restore and Build") {
        dir("${env.REPO_PATH}") {
            bat """
                echo 📦 Restaurando paquetes NuGet...
                nuget restore "${env.REPO_PATH}\\ApiCrmVitalea.sln" -PackagesDirectory "${env.REPO_PATH}\\packages" -DisableParallelProcessing
            """
            compileViewModels()
        }
    }

    stage("Modify Project References") {
        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            powershell '''
                $csprojPath = "ApiCrmVitalea.csproj"
                $xml = [xml](Get-Content $csprojPath)
                $ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
                $ns.AddNamespace("msbuild", "http://schemas.microsoft.com/developer/msbuild/2003")

                # Eliminar referencia a ViewModels.csproj
                $projectReference = $xml.SelectSingleNode("//msbuild:ProjectReference[contains(@Include, 'ViewModels.csproj')]", $ns)
                if ($projectReference) {
                    $projectReference.ParentNode.RemoveChild($projectReference)
                    Write-Host "✅ Referencia al proyecto ViewModels eliminada."
                }

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
                Write-Host "✅ Referencia a DLL ViewModels agregada."
            '''
        }
    }

    stage("Deploy ${api}") {
        def apiConfig = [
            CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE]
        ]

        dir("${env.REPO_PATH}\\ApiCrmVitalea") {
            withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                powershell """
                    [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                    \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }
                    
                    if (-not \$profile) { Write-Error "❌ Perfil MSDeploy no encontrado"; exit 1 }

                    # Variables para MSBuild
                    \$env:MSBuildExtensionsPath = "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\MSBuild"
                    \$env:MSBuildSDKsPath = "${paths.dotnetSdk}"
                    \$env:VSToolsPath = "${paths.vstools}"
                    \$env:MSBuildEnableWorkloadResolver = "false"
                    
                    & "${paths.msbuild}" "ApiCrmVitalea.csproj" `
                        /p:Configuration=${CONFIGURATION} `
                        /p:DeployOnBuild=true `
                        /p:WebPublishMethod=MSDeploy `
                        /p:MsDeployServiceUrl="https://\$(\$profile.publishUrl)/msdeploy.axd" `
                        /p:DeployIisAppPath="\$(\$profile.msdeploySite)" `
                        /p:Username="\$(\$profile.userName)" `
                        /p:Password="\$(\$profile.userPWD)" `
                        /p:AllowUntrustedCertificate=true `
                        /p:VisualStudioVersion=17.0 `
                        /p:VSToolsPath="${paths.vstools}" `
                        /p:BuildProjectReferences=false `
                        /p:SkipResolveProjectReferences=true `
                        /p:TargetFrameworkVersion=v4.7.2 `
                        /maxcpucount `
                        /verbosity:minimal
                """
            }
        }
    }

    stage("Cleanup") {
        dir("${env.REPO_PATH}") {
            restoreCsproj()
        }
    }
}
