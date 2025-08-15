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
                        echo "🔄 Cargando configuración..."
                        def contenido = libraryResource "${config.PRODUCT}.groovy"
                        def configCompleto = evaluate(contenido)
                        def branch = configCompleto.AMBIENTES[config.AMBIENTE].BRANCH
                        echo "🌿 Rama a usar para el despliegue: ${branch}"
                        cloneRepoNET(branch: branch, repoPath: env.REPO_PATH, repoUrl: env.REPO_URL)
                        // Guardar configuración para etapas posteriores
                        env.CONFIG_COMPLETO = configCompleto
                    }
                }
            }

            // Crear un stage por API
            script {
                apis.each { api ->
                    stage("Build ${api}") {
                        steps {
                            script {
                                echo "🛠️ Compilando API: ${api}"
                                def apiConfig = env.CONFIG_COMPLETO.APIS[api]
                                dir("${apiConfig.CS_PROJ_PATH}") {
                                    bat """
                                        dotnet build ${api}.csproj ^
                                            /p:Configuration=${env.CONFIGURATION} ^
                                            /p:Platform="Any CPU"
                                    """
                                }
                            }
                        }
                    }

                    stage("Publish ${api}") {
                        steps {
                            script {
                                echo "📦 Publicando API: ${api}"
                                def apiConfig = env.CONFIG_COMPLETO.APIS[api]

                                dir("${apiConfig.CS_PROJ_PATH}") {
                                    withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                                        bat """
                                            if not exist "%PUBLISH_SETTINGS%" (
                                                echo ❌ ERROR: El archivo de credenciales no existe: %PUBLISH_SETTINGS%
                                                exit /b 1
                                            )
                                            set TEMP_PUBLISH_PROFILE=%WORKSPACE%\\publish_profile.pubxml
                                            copy "%PUBLISH_SETTINGS%" "%TEMP_PUBLISH_PROFILE%"
                                            dotnet msbuild ${api}.csproj ^
                                                /p:DeployOnBuild=true ^
                                                /p:PublishProfile="%TEMP_PUBLISH_PROFILE%" ^
                                                /p:Configuration=${env.CONFIGURATION} ^
                                                /p:Platform="Any CPU"
                                            if exist "%TEMP_PUBLISH_PROFILE%" del "%TEMP_PUBLISH_PROFILE%"
                                        """
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
