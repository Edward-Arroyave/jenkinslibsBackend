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
                                                [xml]$pub = Get-Content "$env:PUBLISH_SETTINGS"
                                                $profile = $pub.publishData.publishProfile | Where-Object { $_.publishMethod -eq "MSDeploy" }

                                                $url  = $profile.publishUrl -replace "^http://", "https://"
                                                $site = $profile.msdeploySite
                                                $user = $profile.userName
                                                $pass = $profile.userPWD

                                                $publishFolder = Join-Path $env:WORKSPACE "publish_${api}"
                                                dotnet restore
                                                dotnet publish -c Release -o $publishFolder

                                                $zipPath = Join-Path $env:WORKSPACE "${api}.zip"
                                                if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
                                                Add-Type -AssemblyName 'System.IO.Compression.FileSystem'
                                                [System.IO.Compression.ZipFile]::CreateFromDirectory($publishFolder, $zipPath)

                                                & "C:\Program Files\IIS\Microsoft Web Deploy V3\msdeploy.exe" `
                                                    -verb:sync `
                                                    -source:package="$zipPath" `
                                                    -dest:contentPath="$site",computerName="$url/msdeploy.axd?site=$site",userName="$user",password="$pass",authType="Basic" `
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
