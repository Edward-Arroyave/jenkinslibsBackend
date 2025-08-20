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

                        cloneRepoNET(branch: branch, repoPath: env.REPO_PATH, repoUrl: env.REPO_URL)

                        // Guardamos configCompleto para el resto
                        env.CONFIG_COMPLETO = groovy.json.JsonOutput.toJson(configCompleto)
                    }
                }
            }

            stage('Deploy APIs') {
                steps {
                    script {
                        def configCompleto = new groovy.json.JsonSlurperClassic().parseText(env.CONFIG_COMPLETO)

                        for (api in apis) {
                            stage("Deploy ${api}") {
                                try {
                                    def apiConfig = [
                                        CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
                                        CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
                                        URL: configCompleto.APIS[api].URL[config.AMBIENTE]
                                    ]

                                    echo "=== Desplegando API: ${api} ==="
                                    echo "Ruta proyecto: ${apiConfig.CS_PROJ_PATH}"
                                    echo "Credenciales usadas: ${apiConfig.CREDENTIALS_ID}"
                                    echo "URL de despliegue: ${apiConfig.URL}"

                                   dir("${apiConfig.CS_PROJ_PATH}") {
                                        withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                                            powershell """
                                             [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

                                                # Buscar el perfil MSDeploy
                                                [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                                                \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }

                                                if (-not \$profile) { Write-Error "❌ No se encontró perfil MSDeploy"; exit 1 }

                                                \$url  = \$profile.publishUrl
                                                \$site = \$profile.msdeploySite
                                                \$user = \$profile.userName
                                                \$pass = \$profile.userPWD
                                                \$projectFolder = (Get-ChildItem -Directory | Select-Object -First 1).FullName  # Carpeta del proyecto

                                                Write-Host "🔄 Restaurando paquetes NuGet..."
                                                dotnet restore

                                                Write-Host "🚀 Publicando ${api} usando Web Deploy..."

                                                &  "C:\Program Files\IIS\Microsoft Web Deploy V3\msdeploy.exe" `
                                                    -verb:sync `
                                                    -source:contentPath="\$projectFolder" `
                                                    -dest:contentPath="\$site",computerName="\$url",userName="\$user",password="\$pass",authType="Basic" `
                                                    -allowUntrusted
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
