def call(Map config) {
    // Convertir API_NAME en lista real si viene como String
    def apis = config.API_NAME
    if (apis instanceof String) {
        apis = apis.split(',').collect { it.trim() }
    }

    def apisExitosas = []
    def apisFallidas = []
    def apiConfig

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
                            try {
                                // Cargar config
                                def contenido = libraryResource "${config.PRODUCT}.groovy"
                                def configCompleto = evaluate(contenido)
                                apiConfig = [
                                    BRANCH: configCompleto.AMBIENTES[config.AMBIENTE].BRANCH,
                                    CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
                                    CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
                                    URL: configCompleto.APIS[api].URL[config.AMBIENTE]
                                ]

                                // Ejecutar pasos por API
                                dir("${apiConfig.CS_PROJ_PATH}") {
                                    cloneRepoNET(branch: apiConfig.BRANCH, repoPath: env.REPO_PATH, repoUrl: env.REPO_URL)
                                    sh "dotnet restore ${api}.csproj"
                                    sh "dotnet build ${api}.csproj --configuration ${env.CONFIGURATION} --no-restore"
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
                    def mensaje = "Despliegue completado en ambiente ${config.AMBIENTE}\n"
                    if (apisExitosas) { mensaje += "✅ Exitosas: ${apisExitosas.join(', ')}\n" }
                    if (apisFallidas) { mensaje += "❌ Fallidas: ${apisFallidas.join(', ')}" }
                    sendNotificationTeamsNET(mensaje)
                }
                cleanWs()
            }
        }
    }
}
