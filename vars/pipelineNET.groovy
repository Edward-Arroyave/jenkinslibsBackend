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
        agent { label 'Windows-node' }
        
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
                        
                        def parallelStages = [:]
                        
                        apis.each { api ->
                            parallelStages["Deploy-${api}"] = {
                                pipelineNET(api, configCompleto, config, CONFIGURATION)
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
                        ENVIRONMENT: config.AMBIENTE
                    ])
                }
                cleanWs()
            }
            
            success { echo '🎉 DESPLIEGUE FINALIZADO CON ÉXITO' }
            failure { echo '💥 DESPLIEGUE FINALIZADO CON ERRORES' }
            unstable { echo '⚠️  DESPLIEGUE FINALIZADO CON ALGUNOS ERRORES' }
        }
    }
}
