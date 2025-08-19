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
            DOTNET_SYSTEM_NET_HTTP_USESOCKETSHTTPHANDLER = "0"
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

                        env.CONFIG_COMPLETO = groovy.json.JsonOutput.toJson(configCompleto)
                    }
                }
            }

            stage('Verificar TLS Config') {
                steps {
                    powershell '''
                        Write-Host "🔍 Forzando TLS 1.2..."
                        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12
                        Write-Host "✅ TLS configurado"
                    '''
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
                                        CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE]
                                    ]

                                    echo "=== Desplegando API: ${api} ==="

                                    dir("${apiConfig.CS_PROJ_PATH}") {
                                        withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                                            powershell """
                                                Write-Host "📄 Restaurando paquetes y compilando ${api}..."
                                                dotnet restore ${api}.csproj
                                                dotnet build ${api}.csproj --configuration \${env:CONFIGURATION} --no-restore

                                                Write-Host "📄 Publicando ${api} usando perfil MSDeploy..."
                                                [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                                                \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }

                                                if (-not \$profile) {
                                                    Write-Error "❌ No se encontró un perfil válido"
                                                    exit 1
                                                }

                                                # Publicar directamente, igual que Visual Studio
                                                dotnet publish ${api}.csproj \\
                                                    --configuration \${env:CONFIGURATION} \\
                                                    --output ./publish \\
                                                    /p:WebPublishMethod=MSDeploy \\
                                                    /p:MsDeployServiceUrl="\$(\$profile.publishUrl)" \\
                                                    /p:DeployIisAppPath="\$(\$profile.msdeploySite)" \\
                                                    /p:UserName="\$(\$profile.userName)" \\
                                                    /p:Password="\$(\$profile.userPWD)" \\
                                                    /p:AllowUntrustedCertificate=true \\
                                                    /p:PrecompileBeforePublish=true \\
                                                    /p:EnableMSDeployAppOffline=true \\
                                                    /p:UseWPP_CopyWebApplication=true \\
                                                    /p:PipelineDependsOnBuild=false
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
                    if (apisExitosas) { APIS_SUCCESSFUL += "✅ ${apisExitosas.join(', ')}\\n" }
                    if (apisFallidas) { APIS_FAILURE += "❌ ${apisFallidas.join(', ')}" }

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