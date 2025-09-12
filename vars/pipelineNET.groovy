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
    def configCompleto // aquí se guarda el mapa de configuración

    pipeline {
        agent { label 'Windows-node' }
        
        environment {
            BUILD_FOLDER = "${env.WORKSPACE}/${config.AMBIENTE}"
            REPO_PATH    = "${BUILD_FOLDER}/repo"
            REPO_URL     = "${config.REPO_URL}"
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
                            configCompleto = evaluate(contenido)   // guardamos el Map en variable global
                            def branch = configCompleto.AMBIENTES[config.AMBIENTE].BRANCH
                            
                            echo "✅ Configuración cargada exitosamente"
                            echo "🌿 Rama configurada: ${branch}"
                            echo "📁 Ruta del repositorio: ${env.REPO_PATH}"
                            
                            cloneRepoNET(branch: branch, repoPath: env.REPO_PATH, repoUrl: env.REPO_URL)
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
                        echo "🎯 Iniciando despliegue de ${apis.size()} APIs en paralelo"
                        
                        def parallelStages = [:]
                        
                        apis.each { api ->
                          parallelStages["Deploy-${api}"] = {
                            try {
                                dir("${configCompleto.APIS[api].REPO_PATH}") {
                                    def csproj = readFile(file: "${api}.csproj")

                                    if (csproj.contains("<TargetFrameworkVersion>v4")) {
                                        echo "⚙️ Proyecto ${api} detectado como .NET Framework 4.x"
                                        deployDotNetFramework4(api, configCompleto, config, CONFIGURATION)
                                    } else {
                                        echo "⚙️ Proyecto ${api} detectado como .NET Core / .NET 5+"
                                        deployDotNet(api, configCompleto, config, CONFIGURATION)
                                    }
                                }
                                def url = configCompleto.APIS[api].URL[config.AMBIENTE]
                                validateApi(url, api)
                                apisExitosas << api
                                echo "🎉 DESPLIEGUE EXITOSO: ${api}"

                            } catch (err) {
                                echo "❌ ERROR EN DESPLIEGUE ${api}: ${err.message}"
                                apisFallidas << api
                                currentBuild.result = 'UNSTABLE'
                            }
                          }
                        }

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
                        ENVIRONMENT: config.AMBIENTE,
                        PRODUCT: config.PRODUCT,
                        WEBHOOK_URL: configCompleto.WEBHOOK_URL   // usamos el Map directo
                    ])
                }
            }
            
            success { echo '🎉 DESPLIEGUE FINALIZADO CON ÉXITO' }
            failure { echo '💥 DESPLIEGUE FINALIZADO CON ERRORES' }
            unstable { echo '⚠️  DESPLIEGUE FINALIZADO CON ALGUNOS ERRORES' }
        }
    }
}
