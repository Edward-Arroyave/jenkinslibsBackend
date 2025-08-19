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

            stage('Configuración TLS Completa') {
                steps {
                    powershell '''
                        Write-Host "🔧 Configurando stack de seguridad completo..."
                        
                        # 1. Forzar TLS 1.2 como protocolo mínimo
                        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12
                        
                        # 2. Habilitar cifrados específicos requeridos por Azure
                        # TLS_RSA_WITH_AES_128_CBC_SHA (0x002F) - Este es el específico que necesita el servidor
                        Write-Host "🔓 Habilitando cifrado TLS_RSA_WITH_AES_128_CBC_SHA..."
                        
                        Add-Type -TypeDefinition @"
                            using System;
                            using System.Net;
                            public static class CipherHelper {
                                public const int TLS_RSA_WITH_AES_128_CBC_SHA = 0x002F;
                                
                                public static void EnableWeakCiphers() {
                                    // Habilitar el cifrado específico requerido
                                    ServicePointManager.SecurityProtocol |= (SecurityProtocolType)TLS_RSA_WITH_AES_128_CBC_SHA;
                                    
                                    // También habilitar otros protocolos comunes para compatibilidad
                                    ServicePointManager.SecurityProtocol |= SecurityProtocolType.Tls;
                                    ServicePointManager.SecurityProtocol |= SecurityProtocolType.Tls11;
                                    ServicePointManager.SecurityProtocol |= SecurityProtocolType.Tls12;
                                }
                            }
"@
                        [CipherHelper]::EnableWeakCiphers()
                        
                        # 3. Configurar políticas adicionales de conexión
                        [System.Net.ServicePointManager]::Expect100Continue = $false
                        [System.Net.ServicePointManager]::CheckCertificateRevocationList = $false
                        
                        # 4. Mostrar configuración final
                        Write-Host "✅ Configuración de seguridad aplicada:"
                        Write-Host "Protocolos habilitados: $([System.Net.ServicePointManager]::SecurityProtocol)"
                        
                        # 5. Verificar conectividad con el endpoint
                        try {
                            Write-Host "🔍 Probando conectividad con Azure..."
                            $testUrl = "https://agendamiento-api-his-co-pruebas.scm.azurewebsites.net"
                            $request = [System.Net.WebRequest]::Create($testUrl)
                            $request.Method = "HEAD"
                            $request.Timeout = 10000
                            
                            $response = $request.GetResponse()
                            Write-Host "✅ Conectividad verificada: $($response.StatusCode)"
                            $response.Close()
                        } catch {
                            Write-Host "⚠️ Advertencia de conectividad: $($_.Exception.Message)"
                        }
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
                                                # Reforzar configuración TLS para esta sesión
                                                [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12
                                                [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 0x00000040
                                                
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

                                                # Publicar con parámetros optimizados
                                                \$publishArgs = @(
                                                    'publish',
                                                    '${api}.csproj',
                                                    '--configuration', "\${env:CONFIGURATION}",
                                                    '--output', './publish',
                                                    '/p:WebPublishMethod=MSDeploy',
                                                    '/p:MsDeployServiceUrl="' + \$profile.publishUrl + '"',
                                                    '/p:DeployIisAppPath="' + \$profile.msdeploySite + '"',
                                                    '/p:UserName="' + \$profile.userName + '"',
                                                    '/p:Password="' + \$profile.userPWD + '"',
                                                    '/p:AllowUntrustedCertificate=true',
                                                    '/p:AuthType=Basic',
                                                    '/p:SkipExtraFilesOnServer=true',
                                                    '/p:MSDeployUseChecksum=true',
                                                    '/p:EnableMSDeployBackup=false',
                                                    '/p:PrecompileBeforePublish=true',
                                                    '/p:EnableMSDeployAppOffline=true',
                                                    '/p:UseWPP_CopyWebApplication=true',
                                                    '/p:PipelineDependsOnBuild=false',
                                                    '/p:DeleteExistingFiles=true'
                                                )

                                                Write-Host "🚀 Ejecutando publicación de ${api}..."
                                                & dotnet @publishArgs
                                                
                                                if (\$LASTEXITCODE -ne 0) {
                                                    Write-Error "❌ Error en la publicación de ${api}"
                                                    exit 1
                                                }
                                                
                                                Write-Host "✅ ${api} publicado exitosamente"
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

            stage('Verificación Final') {
                steps {
                    powershell '''
                        Write-Host "✅ Despliegue completado"
                        Write-Host "📊 Resumen de protocolos habilitados:"
                        Write-Host "$([System.Net.ServicePointManager]::SecurityProtocol)"
                    '''
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
                        APIS_FAILURE: APIS_FAILURE,
                        TLS_CONFIG: "Cifrado TLS_RSA_WITH_AES_128_CBC_SHA habilitado"
                    ])
                }

                cleanWs()
            }
            
            success {
                echo "🎉 Pipeline ejecutado exitosamente"
            }
            
            failure {
                echo "❌ Pipeline falló - Revisar configuración TLS"
            }
        }
    }
}