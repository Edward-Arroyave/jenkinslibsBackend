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
        REPO_URL = 'https://github.com/IT-HEALTH-PROYECTOS-QC/BACKEND_PROYECTO_QC.git'
        CONFIGURATION = 'Release'
        DOTNET_SYSTEM_GLOBALIZATION_INVARIANT = "true"
    }

    stages {
        stage('Clone Repository') {
            steps {
                script {
                    cloneRepo(
                        branch: env.BRANCH,
                        repoPath: env.REPO_PATH,
                        repoUrl: env.REPO_URL
                    )
                }
            }
        }

        stage('Load API Config') {
            steps {
                script {
                    // Cargar el diccionario correspondiente desde resources
                    def configCompleto = load libraryResource("configs/${params.API_NAME}.groovy")
                    // Seleccionar el ambiente: demo o test
                    env.API_CONFIG = configCompleto[params.AMBIENTE]

                    echo "Configuración cargada para API ${params.API_NAME} en ambiente ${params.AMBIENTE}:"
                    echo "Ruta csproj: ${env.API_CONFIG.rutaCsproj}"
                    echo "Credenciales: ${env.API_CONFIG.nombreCredenciales}"
                }
            }
        }

    }
}
