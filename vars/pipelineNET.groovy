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
        }

      stages {
    stage('Load Config & Clone Repo') {
        steps {
            script {
                // carga de configuración
            }
        }
    }

    stage('Build & Publish APIs') {
        steps {
            script {
                apis = config.API_NAME
                if (apis instanceof String) {
                    apis = apis.split(',').collect { it.trim() }
                }

                apis.each { api ->
                    stage("Build ${api}") {  // <- stage dinámico no permitido aquí
                        steps {
                            dir("${env.CONFIG_COMPLETO.APIS[api].CS_PROJ_PATH}") {
                                bat "dotnet build ${api}.csproj ..."
                            }
                        }
                    }

                    stage("Publish ${api}") {
                        steps {
                            dir("${env.CONFIG_COMPLETO.APIS[api].CS_PROJ_PATH}") {
                                withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                                    bat "dotnet msbuild ..."
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


        post {
            always {
                script {
                    def APIS_FAILURE = ""
                    def APIS_SUCCESSFUL = ""
                    if (apisExitosas) { APIS_SUCCESSFUL += "✅ ${apisExitosas.join(', ')}\n" }
                    if (apisFallidas) { APIS_FAILURE    += "❌ ${apisFallidas.join(', ')}" }

                    sendNotificationTeamsNET([
                        APIS_SUCCESSFUL: APIS_SUCCESSFUL,
                        APIS_FAILURE: APIS_FAILURE
                    ])
                }

                cleanWs()
            }
        }
    }
}
