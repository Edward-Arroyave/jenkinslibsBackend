def call(Map config) {
    def apiConfig
    def apis = config.API_NAME   // ahora es la lista de APIs seleccionadas
    def apisDesplegadas = []    // para registrar las APIs que se desplegaron

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
                            echo "=== Desplegando ${api} ==="

                            // Cargar configuración para esta API
                            def contenido = libraryResource "${config.PRODUCT}.groovy"
                            def configCompleto = evaluate(contenido)
                            apiConfig = [
                                BRANCH: configCompleto.AMBIENTES[config.AMBIENTE].BRANCH,
                                CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
                                CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
                                URL: configCompleto.APIS[api].URL[config.AMBIENTE]
                            ]

                            echo "Configuración cargada para ${api} en ambiente ${config.AMBIENTE}"

                            // Aquí van todos los stages por API
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

                            // Registrar API exitosa
                            apisDesplegadas << api
                        }
                    }
                }
            }
        }

        post {
            always {
                script {
                    // Aquí envías UN SOLO mensaje con todas las APIs que se desplegaron
                    def mensaje = "✅ Despliegue completado para las APIs: ${apisDesplegadas.join(', ')} en ambiente ${config.AMBIENTE}"
                    echo mensaje
                    sendNotificationTeamsNET(mensaje)
                }
                cleanWs()
            }
        }
    }
}
