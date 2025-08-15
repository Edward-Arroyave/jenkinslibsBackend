def call(Map config) {
    def apis = config.API_NAME
    if (apis instanceof String) {
        apis = apis.split(',').collect { it.trim() }
    }

    echo "APIs seleccionadas para despliegue: ${apis.join(', ')}"

    def apisExitosas = []
    def apisFallidas = []

    pipeline {
        agent {
            label 'Windows-node'
        }

        environment {
            BUILD_FOLDER = "${env.WORKSPACE}/${env.BUILD_ID}"
            REPO_PATH = "${BUILD_FOLDER}/repo"
            REPO_URL = "${config.REPO_URL}"
            CONFIGURATION = 'Release'
            DOTNET_SYSTEM_GLOBALIZATION_INVARIANT = "true"
            PUBLISH_OUTPUT_DIR = "${BUILD_FOLDER}/publish"
        }

        stages {
            stage('Preparar entorno') {
                steps {
                    script {
                        // Clonar repositorio si es necesario
                        bat "mkdir \"${PUBLISH_OUTPUT_DIR}\""
                    }
                }
            }

            stage('Procesar APIs') {
                steps {
                    script {
                        apis.each { apiName ->
                            try {
                                // Obtener configuración específica de la API
                                def apiConfig = env.CONFIG_COMPLETO.APIS[apiName]
                                
                                // Stage de Build
                                echo "Building ${apiName}..."
                                dir(apiConfig.CS_PROJ_PATH) {
                                    bat """
                                        dotnet clean "${apiName}.csproj" -c ${CONFIGURATION}
                                        dotnet build "${apiName}.csproj" -c ${CONFIGURATION} --no-restore
                                    """
                                }

                                // Stage de Publish
                                echo "Publishing ${apiName}..."
                                dir(apiConfig.CS_PROJ_PATH) {
                                    withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_PROFILE')]) {
                                        bat """
                                            dotnet publish "${apiName}.csproj" \
                                            -c ${CONFIGURATION} \
                                            --no-build \
                                            -p:PublishProfile=${PUBLISH_PROFILE} \
                                            -p:DeployOnBuild=true \
                                            -p:WebPublishMethod=FileSystem \
                                            -p:DeleteExistingFiles=true \
                                            -p:publishUrl=${PUBLISH_OUTPUT_DIR}/${apiName}
                                        """
                                    }
                                }
                                apisExitosas << apiName
                            } catch (Exception e) {
                                apisFallidas << apiName
                                echo "Error procesando ${apiName}: ${e.toString()}"
                                // Continúa con las siguientes APIs
                            }
                        }
                    }
                }
            }

            stage('Despliegue') {
                when {
                    expression { apisExitosas.size() > 0 }
                }
                steps {
                    script {
                        // Aquí iría la lógica para copiar los archivos publicados al servidor destino
                        // Por ejemplo:
                        // bat "xcopy \"${PUBLISH_OUTPUT_DIR}\\*\" \"\\\\server\\share\\\" /E /Y /I"
                        echo "Archivos listos en ${PUBLISH_OUTPUT_DIR}"
                    }
                }
            }
        }

        post {
            always {
                script {
                    cleanWs()
                }
            }
            success {
                script {
                    if (apisExitosas) {
                        sendNotificationTeamsNET([
                            APIS_SUCCESSFUL: "✅ ${apisExitosas.join(', ')}",
                            APIS_FAILURE: apisFallidas ? "❌ ${apisFallidas.join(', ')}" : "N/A"
                        ])
                    }
                }
            }
            failure {
                script {
                    sendNotificationTeamsNET([
                        APIS_SUCCESSFUL: apisExitosas ? "✅ ${apisExitosas.join(', ')}" : "N/A",
                        APIS_FAILURE: "❌ ${apisFallidas.join(', ')}"
                    ])
                }
            }
        }
    }
}