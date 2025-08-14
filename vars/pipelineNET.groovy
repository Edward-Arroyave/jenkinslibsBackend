def call(Map config) {
    // Convertir API_NAME en lista real si viene como String
    def apis = config.API_NAME
    if (apis instanceof String) {
        apis = apis.split(',').collect { it.trim() }
    }

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
            stage('Load Config & Clone Repo') {
                steps {
                    script {
                        echo "🔄 Cargando configuración y clonando repositorio..."
                        def contenido = libraryResource "${config.PRODUCT}.groovy"
                        def configCompleto = evaluate(contenido)

                        // Solo tomar la rama por ambiente
                        def branch = configCompleto.AMBIENTES[config.AMBIENTE].BRANCH

                        stage("Clone Repository ${branch}") {
                            cloneRepoNET(branch: branch, repoPath: env.REPO_PATH, repoUrl: env.REPO_URL)
                        }

                        // Guardamos config completo para usar después en cada API
                        env.CONFIG_COMPLETO = configCompleto
                    }
                }
            }

            stage('Deploy APIs') {
                steps {
                    script {
                        def configCompleto = env.CONFIG_COMPLETO
                        for (api in apis) {
                            echo "=== Desplegando API: ${api} ==="
                            try {
                                def apiConfig = [
                                    CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
                                    CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
                                    URL: configCompleto.APIS[api].URL[config.AMBIENTE]
                                ]

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
