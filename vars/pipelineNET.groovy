def call(Map config) {
    def apiConfig
    def apis = config.API_NAME               // lista de APIs seleccionadas
    def apisExitosas = []
    def apisFallidas = []

    pipeline {
        agent {
            docker {
                image 'mcr.microsoft.com/dotnet/sdk:8.0'
                args '-u root:root'
                label 'docker-node'
            }
        }

        environment {
            BUILD_FOLDER = "${env.WORKSPACE}/${env.BUILD_ID}"
            REPO_PATH = "${BUILD_FOLDER}/repo"
            REPO_URL = 'https://github.com/IT-HEALTH/HIS_ITHEALTH_BACK.git'
            CONFIGURATION = 'Release'
            DOTNET_SYSTEM_GLOBALIZATION_INVARIANT = "true"
        }

        stages {
            stage('Desplegar APIs') {
                steps {
                    script {
                        for (api in apis) {
                            echo "=== Iniciando despliegue para ${api} ==="
                            try {
                                // Cargar configuración para esta API
                                def contenido = libraryResource "${config.PRODUCT}.groovy"
                                def configCompleto = evaluate(contenido)
                                apiConfig = [
                                    BRANCH: configCompleto.AMBIENTES[config.AMBIENTE].BRANCH,
                                    CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
                                    CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
                                    URL: configCompleto.APIS[api].URL[config.AMBIENTE]
                                ]

                                stage("Clone ${api}") {
                                    cloneRepoNET(branch: apiConfig.BRANCH, repoPath: env.REPO_PATH, repoUrl: env.REPO_URL)
                                }

                                stage("Restore ${api}") {
                                    dir("${apiConfig.CS_PROJ_PATH}") {
                                        sh "dotnet restore ${api}.csproj"
                                    }
                                }

                                stage("Build ${api}") {
                                    dir("${apiConfig.CS_PROJ_PATH}") {
                                        sh "dotnet build ${api}.csproj --configuration ${env.CONFIGURATION} --no-restore"
                                    }
                                }

                                stage("Publish ${api}") {
                                    dir("${apiConfig.CS_PROJ_PATH}") {
                                        withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                                            sh """
                                                TEMP_PUBLISH_PROFILE=\$(mktemp)
                                                cp "\$PUBLISH_SETTINGS" "\$TEMP_PUBLISH_PROFILE"

                                                dotnet msbuild ${api}.csproj \
                                                    /p:DeployOnBuild=true \
                                                    /p:PublishProfile="\$TEMP_PUBLISH_PROFILE" \
                                                    /p:Configuration=${env.CONFIGURATION} \
                                                    /p:Platform="Any CPU"

                                                rm -f "\$TEMP_PUBLISH_PROFILE"
                                            """
                                        }
                                    }
                                }

                                // Si todo OK
                                apisExitosas << api
                                echo "✅ Despliegue exitoso de ${api}"
                            } catch (err) {
                                apisFallidas << api
                                echo "❌ Error en despliegue de ${api}: ${err}"
                                // opcional: registrar log o enviar alerta parcial
                            }
                        }
                    }
                }
            }
        }

        post {
            always {
                script {
                    def mensaje = "Despliegue completado en ambiente ${config.AMBIENTE}\n"
                    if (apisExitosas) {
                        mensaje += "✅ APIs exitosas: ${apisExitosas.join(', ')}\n"
                    }
                    if (apisFallidas) {
                        mensaje += "❌ APIs fallidas: ${apisFallidas.join(', ')}"
                    }
                    sendNotificationTeamsNET(mensaje)
                }
                cleanWs()
            }
        }
    }
}
