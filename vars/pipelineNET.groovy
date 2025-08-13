def call(Map config) {

    pipeline {
        agent {
            docker {
                image 'mcr.microsoft.com/dotnet/sdk:8.0'  // La imagen que usarás
                args '-u root:root'                      // Opcional, para permisos root
                label 'docker-node'                      // Nodo donde tienes Docker instalado
            }
        }

        environment {
            BUILD_FOLDER = "${env.WORKSPACE}/${env.BUILD_ID}"
            REPO_PATH = "${BUILD_FOLDER}/repo"
            REPO_URL = 'https://github.com/IT-HEALTH/HIS_ITHEALTH_FRONT.git'
            CONFIGURATION = 'Release'
            DOTNET_SYSTEM_GLOBALIZATION_INVARIANT = "true"
        }
            stages{
                    stage('Load API Config') {
                        steps {
                            script {
                                // Cargar como texto
                                def contenido = libraryResource "${config.PRODUCT}/${config.API_NAME}.groovy"

                                // Interpretar el contenido como código Groovy (esto devuelve un Map)
                                def configCompleto = evaluate(contenido)

                                // Seleccionar el ambiente: demo o test
                                def apiConfig = configCompleto[config.AMBIENTE]

                                

                                echo "Configuración cargada para API ${config.API_NAME} en ambiente ${config.AMBIENTE}:"
                                echo "Ruta csproj: ${apiConfig.CS_PROJ_PATH}"
                                echo "Credenciales: ${apiConfig.CREDENTIALS_ID}"
                                echo "Rama: ${apiConfig.BRANCH}" 
                            }
                        }   
                    }
                    stage('Clone Repository') {
                        steps {
                            script {
                                cloneRepo(
                                    branch: apiConfig.BRANCH,
                                    repoPath: env.REPO_PATH,
                                    repoUrl: env.REPO_URL
                                )
                            }
                        }
                    }

                    stage('Restore Packages') {
                        steps {
                            dir("${apiConfig.CS_PROJ_PATH}") {
                                sh "dotnet restore ${config.API_NAME}.csproj"
                            }
                        }
                    }

                    stage('Build Project') {
                        steps {
                            dir("${apiConfig.CS_PROJ_PATH}") {
                                sh "dotnet build ${config.API_NAME}.csproj --configuration ${env.CONFIGURATION} --no-restore"
                            }
                        }
                    }

                    stage('Publish Project') {
                        steps {
                            dir("${apiConfig.CS_PROJ_PATH}") {
                                withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                                    sh """
                                        TEMP_PUBLISH_PROFILE=\$(mktemp)
                                        cp "\$PUBLISH_SETTINGS" "\$TEMP_PUBLISH_PROFILE"

                                        dotnet msbuild ${config.API_NAME}.csproj \
                                            /p:DeployOnBuild=true \
                                            /p:PublishProfile="\$TEMP_PUBLISH_PROFILE" \
                                            /p:Configuration=${env.CONFIGURATION} \
                                            /p:Platform="Any CPU"

                                        rm -f "\$TEMP_PUBLISH_PROFILE"
                                    """
                                }
                            }
                        }
                    }
                }

                post {
                    failure {
                        echo "❌ Despliegue de ${params.API_NAME} fallido."
                    }
                    success {
                        echo "✅ Despliegue de ${params.API_NAME} exitoso."
                    }
                }
            }
}