def call(Map config) {
    def apis = config.API_NAME
    if (apis instanceof String) {
        apis = apis.split(',').collect { it.trim() }
    }
    
    echo "🚀 =============================== INICIO DESPLIEGUE ==============================="
    echo "📋 APIs seleccionadas para despliegue: ${apis.join(', ')}"
    echo "🌍 Ambiente: ${config.AMBIENTE}"
    echo "📦 Producto: ${config.PRODUCT}"
    
    def apisExitosas = []
    def apisFallidas = []

    pipeline {
        agent { label 'Windws-node' }
        
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
                        echo "🔄 Cargando configuración desde: ${config.PRODUCT}.groovy"
                        
                        try {
                            def contenido = libraryResource "${config.PRODUCT}.groovy"
                            def configCompleto = evaluate(contenido)
                            def branch = configCompleto.AMBIENTES[config.AMBIENTE].BRANCH
                            
                            echo "✅ Configuración cargada exitosamente"
                            echo "🌿 Rama configurada: ${branch}"
                            echo "📁 Ruta del repositorio: ${env.REPO_PATH}"
                            
                            cloneRepoNET(branch: branch, repoPath: env.REPO_PATH, repoUrl: env.REPO_URL)
                            
                            // Guardamos configCompleto para el resto
                            env.CONFIG_COMPLETO = groovy.json.JsonOutput.toJson(configCompleto)
                            
                        } catch (Exception e) {
                            echo "❌ ERROR: No se pudo cargar la configuración"
                            echo "📝 Detalles: ${e.message}"
                            error("Fallo en carga de configuración")
                        }
                    }
                }
            }

            stage('Deploy APIs') {
                steps {
                    script {
                        def configCompleto = new groovy.json.JsonSlurperClassic().parseText(env.CONFIG_COMPLETO)
                        
                        echo "🎯 Iniciando despliegue de ${apis.size()} APIs en paralelo"
                        
                        // Crear mapa para etapas paralelas
                        def parallelStages = [:]
                        
                        apis.each { api ->
                            parallelStages["Deploy-${api}"] = {
                                try {
                                    echo "🔹 ========== INICIANDO DESPLIEGUE: ${api} =========="
                                    
                                    // Stage de Restore
                                    stage("Restore ${api}") {
                                        dir("${configCompleto.APIS[api].REPO_PATH}") {
                                            echo "📦 Restaurando dependencias para: ${api}"
                                            echo "📁 Directorio: ${configCompleto.APIS[api].REPO_PATH}"
                                            
                                            powershell """
                                            Write-Host "📄 Restaurando dependencias de ${api}..."
                                            dotnet restore ${api}.csproj --verbosity normal
                                            """
                                            
                                            echo "✅ Restore completado para: ${api}"
                                        }
                                    }

                                    // Stage de Deploy
                                    stage("Deploy ${api}") {
                                        def apiConfig = [
                                            CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
                                            CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
                                            URL: configCompleto.APIS[api].URL[config.AMBIENTE]
                                        ]
                                        
                                        echo "🌐 === CONFIGURACIÓN DESPLIEGUE ${api} ==="
                                        echo "📂 Ruta proyecto: ${apiConfig.CS_PROJ_PATH}"
                                        echo "🔑 Credenciales: ${apiConfig.CREDENTIALS_ID}"
                                        echo "🌍 URL destino: ${apiConfig.URL}"
                                        echo "⚙️  Configuración: ${CONFIGURATION}"
                                        
                                        dir("${apiConfig.CS_PROJ_PATH}") {
                                            withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
                                                powershell """
                                                Write-Host "📋 Leyendo perfil de publicación..."
                                                [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                                                \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }
                                                
                                                if (-not \$profile) {
                                                    Write-Error "❌ No se encontró un perfil válido de MSDeploy"
                                                    exit 1
                                                }
                                                
                                                Write-Host "✅ Perfil encontrado: \$(\$profile.profileName)"
                                                Write-Host "🔗 URL: \$(\$profile.publishUrl)"
                                                Write-Host "🏗️ Sitio: \$(\$profile.msdeploySite)"
                                                
                                                \$url = \$profile.publishUrl
                                                \$site = \$profile.msdeploySite
                                                \$user = \$profile.userName
                                                \$pass = \$profile.userPWD
                                                
                                                \$projectFile = (Get-ChildItem -Filter "*.csproj").FullName
                                                if (-not \$projectFile) {
                                                    Write-Error "❌ No se encontró el archivo .csproj"
                                                    exit 1
                                                }
                                                
                                                Write-Host "🚀 Iniciando publicación de: \$projectFile"
                                                
                                                # Ejecuta msbuild con las variables de entorno configuradas
                                                dotnet msbuild "\$projectFile" /p:DeployOnBuild=true /p:WebPublishMethod=MSDeploy /p:MsDeployServiceUrl="\$url" /p:DeployIisAppPath="\$site" /p:UserName="\$user" /p:Password="\$pass" /p:Configuration=${CONFIGURATION} /p:AllowUntrustedCertificate=true /verbosity:normal
                                                
                                                Write-Host "✅ Publicación completada exitosamente"
                                                """
                                            }
                                        }
                                        
                                        apisExitosas << api
                                        echo "🎉 DESPLIEGUE EXITOSO: ${api}"
                                    }
                                    
                                } catch (err) {
                                    echo "❌ ERROR EN DESPLIEGUE ${api}: ${err.message}"
                                    echo "📝 Stack trace: ${err.stackTrace}"
                                    apisFallidas << api
                                    currentBuild.result = 'UNSTABLE' // Marcar como inestable pero continuar
                                }
                                
                                echo "🔸 ========== FIN DESPLIEGUE: ${api} =========="
                            }
                        }

                        // Ejecutar todas las etapas en paralelo
                        echo "⏰ Ejecutando despliegues en paralelo..."
                        parallel parallelStages
                    }
                }
            }
        }
        
        post {
            always {
                script {
                    echo "📊 =============================== RESUMEN DESPLIEGUE ==============================="
                    echo "✅ APIs exitosas: ${apisExitosas.size()}/${apis.size()}"
                    echo "❌ APIs fallidas: ${apisFallidas.size()}/${apis.size()}"
                    
                    // CORRECCIÓN: Las sentencias if deben estar dentro de echo o steps
                    if (apisExitosas) {
                        echo "🎯 Exitosas: ${apisExitosas.join(', ')}"
                    } else {
                        echo "⚠️  No hubo APIs exitosas"
                    }
                    
                    if (apisFallidas) {
                        echo "💥 Fallidas: ${apisFallidas.join(', ')}"
                    } else {
                        echo "✅ Todas las APIs fueron exitosas"
                    }
                    
                    echo "⏰ Duración total: ${currentBuild.durationString}"

                    sendNotificationTeamsNET([
                        APIS_SUCCESSFUL:  apisExitosas.join(', '),
                        APIS_FAILURE: apisFallidas.join(', '),
                        ENVIRONMENT: config.AMBIENTE
                    ])
                    
                    echo "📧 Notificación enviada a Teams"
                }
            }
            
            success {
                echo '🎉 DESPLIEGUE FINALIZADO CON ÉXITO'
            }
            
            failure {
                echo '💥 DESPLIEGUE FINALIZADO CON ERRORES'
            }
            
            unstable {
                echo '⚠️  DESPLIEGUE FINALIZADO CON ALGUNOS ERRORES'
            }
            
       
          
        }
    }
}