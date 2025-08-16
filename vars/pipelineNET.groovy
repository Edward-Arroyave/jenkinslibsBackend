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

                                stage("Deploy ${api}") {
                                    dir("${apiConfig.CS_PROJ_PATH}") {
                                        withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                                            powershell """ 
                                                Write-Host "📄 Leyendo perfil de publicación desde: \$env:PUBLISH_SETTINGS"

                                                # Cargar el archivo de publicación
                                                [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                                                \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }

                                                if (-not \$profile) {
                                                    Write-Error "❌ No se encontró un perfil con publishMethod=MSDeploy en \$env:PUBLISH_SETTINGS"
                                                    exit 1
                                                }

                                                Write-Host "🔑 Usando perfil: \$(\$profile.profileName)"

                                                # Variables
                                                \$url  = \$profile.publishUrl
                                                \$site = \$profile.msdeploySite
                                                \$user = \$profile.userName
                                                \$pass = \$profile.userPWD

                                                # Get the actual project file name
                                                \$projectFile = (Get-ChildItem -Filter "*.csproj").FullName
                                                if (-not \$projectFile) {
                                                    Write-Error "❌ No se encontró ningún archivo .csproj en el directorio actual"
                                                    exit 1
                                                }

                                                Write-Host "🚀 Iniciando despliegue completo (restore, build, publish) del proyecto: \$projectFile"

                                                # Ejecutar todo en un solo comando MSBuild
                                                dotnet msbuild "\$projectFile" `
                                                    /p:DeployOnBuild=true `
                                                    /p:WebPublishMethod=MSDeploy `
                                                    /p:MsDeployServiceUrl="\$url" `
                                                    /p:DeployIisAppPath="\$site" `
                                                    /p:UserName="\$user" `
                                                    /p:Password="\$pass" `
                                                    /p:Configuration=${CONFIGURATION} `
                                                    /p:AllowUntrustedCertificate=true `
                                                    /p:RestorePackagesConfig=true `
                                                    /p:RestoreDuringBuild=true `
                                                    /t:Build `
                                                    /p:BuildProjectReferences=true
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
    

