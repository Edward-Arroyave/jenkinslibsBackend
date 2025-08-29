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
                                try {
                                    echo "🔹 ========== INICIANDO DESPLIEGUE: ${api} =========="
                                    
                                    dir("${configCompleto.APIS[api].REPO_PATH}") {
                                        def csproj = readFile(file: "${api}.csproj")
                                        if (csproj.contains("<TargetFrameworkVersion>v4")) {


                                            echo "⚙️ Proyecto ${api} detectado como .NET Framework 4.x"

                                            def msbuildPath = "C:\\Windows\\Microsoft.NET\\Framework\\v4.0.30319\\msbuild.exe"


                                            stage("Restore ${api} (.NET 4.x)") {
                                                bat """
                                                    echo 📦 Restaurando paquetes NuGet para ${api}...
                                                    nuget restore ${api}.csproj -PackagesDirectory ..\\packages
                                                """
                                            }
                                            stage("Deploy ${api} (.NET 4.x)") {
                                                def apiConfig = [
                                                    CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
                                                    CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
                                                    URL: configCompleto.APIS[api].URL[config.AMBIENTE]
                                                ]

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
                                                            
                                                            Write-Host "🚀 Publicando: \$projectFile"
                                                            
                                                            # USAR MSBUILD EN LUGAR DE DOTNET MSBUILD PARA .NET FRAMEWORK 4.x
                                                            & "${msbuildPath}" "\$projectFile" /p:DeployOnBuild=true /p:WebPublishMethod=MSDeploy /p:MsDeployServiceUrl="\$url" /p:DeployIisAppPath="\$site" /p:UserName="\$user" /p:Password="\$pass" /p:Configuration=${CONFIGURATION} /p:AllowUntrustedCertificate=true /verbosity:normal /p:VisualStudioVersion=16.0
                                                        """
                                                    }
                                                }
                                            }
                                        } else {
                                            echo "⚙️ Proyecto ${api} detectado como .NET Core / .NET 5+"

                                            stage("Restore ${api}") {
                                                powershell """
                                                    Write-Host "📄 Restaurando dependencias de ${api}..."
                                                    dotnet restore ${api}.csproj --verbosity normal
                                                """
                                            }

                                            stage("Deploy ${api}") {
                                                def apiConfig = [
                                                    CS_PROJ_PATH: configCompleto.APIS[api].REPO_PATH,
                                                    CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE],
                                                    URL: configCompleto.APIS[api].URL[config.AMBIENTE]
                                                ]

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
                                                            
                                                            Write-Host "🚀 Publicando: \$projectFile"
                                                            
                                                            dotnet msbuild "\$projectFile" /p:DeployOnBuild=true /p:WebPublishMethod=MSDeploy /p:MsDeployServiceUrl="\$url" /p:DeployIisAppPath="\$site" /p:UserName="\$user" /p:Password="\$pass" /p:Configuration=${CONFIGURATION} /p:AllowUntrustedCertificate=true /verbosity:normal
                                                        """
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    apisExitosas << api
                                    echo "🎉 DESPLIEGUE EXITOSO: ${api}"
                                    
                                } catch (err) {
                                    echo "❌ ERROR EN DESPLIEGUE ${api}: ${err.message}"
                                    apisFallidas << api
                                    currentBuild.result = 'UNSTABLE'
                                }
                                
                                echo "🔸 ========== FIN DESPLIEGUE: ${api} =========="
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
            }
            
            success { echo '🎉 DESPLIEGUE FINALIZADO CON ÉXITO' }
            failure { echo '💥 DESPLIEGUE FINALIZADO CON ERRORES' }
            unstable { echo '⚠️  DESPLIEGUE FINALIZADO CON ALGUNOS ERRORES' }
        }
    }
}
