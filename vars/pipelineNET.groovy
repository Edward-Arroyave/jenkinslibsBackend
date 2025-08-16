def call(Map config) {
    def apis = config.API_NAME instanceof String ? config.API_NAME.split(',').collect { it.trim() } : config.API_NAME

    echo "APIs seleccionadas para despliegue: ${apis.join(', ')}"

    def apisExitosas = []
    def apisFallidas = []
    def configCompleto

    pipeline {
        agent { label 'Windws-node' }

        environment {
            BUILD_FOLDER = "${env.WORKSPACE}/${env.BUILD_ID}"
            REPO_PATH = "${BUILD_FOLDER}/repo"
            REPO_URL = "${config.REPO_URL}"
            CONFIGURATION = 'Release'
            DOTNET_SYSTEM_GLOBALIZATION_INVARIANT = "true"
        }

        stages {
            stage('Load Config & Clone Repo') {
                steps {
                    script {
                        echo "🔄 Cargando configuración..."
                        configCompleto = evaluate(libraryResource("${config.PRODUCT}.groovy"))

                        def branch = configCompleto.AMBIENTES[config.AMBIENTE].BRANCH
                        echo "🌿 Rama a usar para el despliegue: ${branch}"

                        cloneRepoNET(branch: branch, repoPath: env.REPO_PATH, repoUrl: env.REPO_URL)
                    }
                }
            }

            stage('Deploy APIs') {
                steps {
                    script {
                        apis.each { api ->
                            echo "\n🚀 Desplegando API: ${api}"
                            try {
                                def apiConfig = configCompleto.APIS[api]
                                def csProjPath = apiConfig.REPO_PATH
                                def credencial = apiConfig.CREDENCIALES[config.AMBIENTE]
                                def url        = apiConfig.URL[config.AMBIENTE]

                                echo "Ruta proyecto: ${csProjPath}"
                                echo "Credenciales usadas: ${credencial}"
                                echo "URL de despliegue: ${url}"

                                dir(csProjPath) {
                                    withCredentials([file(credentialsId: credencial, variable: 'PUBLISH_SETTINGS')]) {
                                        powershell """
                                            Write-Host "📦 Restaurando y compilando..."
                                            dotnet restore *.csproj
                                            dotnet build *.csproj --configuration ${env.CONFIGURATION} --no-restore
                                            
                                            Write-Host "📄 Leyendo perfil de publicación desde: \$env:PUBLISH_SETTINGS"
                                            [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                                            \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }
                                            if (-not \$profile) { throw '❌ No se encontró un perfil válido' }

                                            Write-Host "🔑 Usando perfil: \$(\$profile.profileName)"
                                            \$projectFile = (Get-ChildItem -Filter "*.csproj").FullName
                                            if (-not \$projectFile) { throw '❌ No se encontró el archivo .csproj' }

                                            Write-Host "🏗 Publicando proyecto: \$projectFile"
                                            dotnet msbuild "\$projectFile" `
                                                /p:DeployOnBuild=true `
                                                /p:WebPublishMethod=MSDeploy `
                                                /p:MsDeployServiceUrl="\$($profile.publishUrl)" `
                                                /p:DeployIisAppPath="\$($profile.msdeploySite)" `
                                                /p:UserName="\$($profile.userName)" `
                                                /p:Password="\$($profile.userPWD)" `
                                                /p:Configuration=${CONFIGURATION} `
                                                /p:AllowUntrustedCertificate=true
                                        """
                                    }
                                }
                                apisExitosas << api
                            } catch (err) {
                                echo "❌ Error en ${api}: ${err}"
                                apisFallidas << api
                            }
                        }
                    }
                }
            }
        }

        post {
            always {
                script {
                    def resumenOk = apisExitosas ? "✅ ${apisExitosas.join(', ')}\n" : ""
                    def resumenFail = apisFallidas ? "❌ ${apisFallidas.join(', ')}" : ""

                    sendNotificationTeamsNET([
                        APIS_SUCCESSFUL: resumenOk,
                        APIS_FAILURE: resumenFail
                    ])
                }
                cleanWs()
            }
        }
    }
}
