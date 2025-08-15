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
            label 'Windws-node'
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
                                    bat "dotnet restore ${api}.csproj"
                                }
                            }

                            stage("Build ${api}") {
                                dir("${apiConfig.CS_PROJ_PATH}") {
                                    bat "dotnet build ${api}.csproj --configuration ${env.CONFIGURATION} --no-restore"
                                }
                            }

                            stage("Publish ${api}") {
                                dir("${apiConfig.CS_PROJ_PATH}") {
                                    withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                                        bat """
                                            :: Aseguramos que la carpeta temporal exista
                                            if not exist "%TEMP%" mkdir "%TEMP%"

                                            set TEMP_PUBLISH_PROFILE=%TEMP%\\temp_pubxml.pubxml

                                            :: Copiamos el PublishSettings
                                            copy /Y "%PUBLISH_SETTINGS%" "%TEMP_PUBLISH_PROFILE%"

                                            :: Verificamos que el archivo exista
                                            if not exist "%TEMP_PUBLISH_PROFILE%" (
                                                echo ❌ No se pudo copiar el PublishSettings
                                                exit /b 1
                                            ) else (
                                                echo ✅ Archivo de publicación listo: %TEMP_PUBLISH_PROFILE%
                                            )

                                            :: Ejecutamos dotnet msbuild
                                            dotnet msbuild ${api}.csproj ^
                                                /p:DeployOnBuild=true ^
                                                /p:PublishProfile="%TEMP_PUBLISH_PROFILE%" ^
                                                /p:Configuration=${env.CONFIGURATION} ^
                                                /p:Platform="Any CPU"

                                            :: Limpiamos el archivo temporal
                                            del /Q "%TEMP_PUBLISH_PROFILE%"
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
    

