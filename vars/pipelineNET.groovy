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
            docker {
                image config.DOCKER_IMAGE
                args '-u root:root'
                label 'docker-node'
            }
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
                        echo "🔄 Cargando configuración..."
                        def contenido = libraryResource "${config.PRODUCT}.groovy"
                        def configCompleto = evaluate(contenido)

                        def branch = configCompleto.AMBIENTES[config.AMBIENTE].BRANCH
                        echo "🌿 Rama a usar para el despliegue: ${branch}"

                        stage("Clone Repository ${branch}") {
                            cloneRepoNET(branch: branch, repoPath: env.REPO_PATH, repoUrl: env.REPO_URL)
                        }

                        // Guardamos configCompleto en variable local para usar después
                        return configCompleto
                    }
                }
            }

            stage('Deploy APIs') {
                steps {
                    script {
                        def configCompleto = evaluate(libraryResource("${config.PRODUCT}.groovy"))

                        for (api in apis) {
                            echo "=== Desplegando API: ${api} ==="
                            try {
                                def apiConfig = [
                                    CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
                                    CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
                                    URL: configCompleto.APIS[api].URL[config.AMBIENTE]
                                ]

                                echo "Ruta proyecto: ${apiConfig.CS_PROJ_PATH}"
                                echo "Credenciales usadas: ${apiConfig.CREDENTIALS_ID}"
                                echo "URL de despliegue: ${apiConfig.URL}"

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
                                        withCredentials([
                                            usernamePassword(
                                                credentialsId: apiConfig.CREDENTIALS_ID,
                                                usernameVariable: 'FTP_USER',
                                                passwordVariable: 'FTP_PASS'
                                            )
                                        ]) {
                                            sh """
                                                # Crear directorio temporal de publicación
                                                PUBLISH_DIR=\$(mktemp -d)
                                                
                                                # Publicar los archivos localmente
                                                dotnet publish ${api}.csproj -c ${env.CONFIGURATION} -o "\$PUBLISH_DIR"

                                                # Configura manualmente el host y la ruta remota del servidor FTP
                                                FTP_HOST=ftp.tu-servidor.com
                                                REMOTE_PATH=/ruta/remota/del/proyecto

                                                echo "Subiendo archivos vía FTP a \$FTP_HOST, ruta: \$REMOTE_PATH"

                                                # Subir archivos con lftp
                                                lftp -u "\$FTP_USER","\$FTP_PASS" \$FTP_HOST <<EOF
                                                mirror -R "\$PUBLISH_DIR" "\$REMOTE_PATH"
                                                quit
                                EOF
                                                # Limpiar directorio temporal
                                                rm -rf "\$PUBLISH_DIR"
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
                    def APIS_FAILURE = ""
                    def APIS_SUCCESSFUL = ""
                    if (apisExitosas) { APIS_SUCCESSFUL += "✅ ${apisExitosas.join(', ')}\n" }
                    if (apisFallidas) { APIS_FAILURE    += "❌ ${apisFallidas.join(', ')}" }

                    // Llamada correcta
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
    

