stage("Deploy ${api}") {
    def apiConfig = [
        CREDENTIALS_ID: configCompleto.APIS[api].CREDENCIALES[config.AMBIENTE]
    ]

    dir("${env.REPO_PATH}\\ApiCrmVitalea") {
        withCredentials([file(credentialsId: apiConfig.CREDENTIALS_ID, variable: 'PUBLISH_SETTINGS')]) {
            powershell """
                Write-Host "🚀 [Deploy] Publicando API ${api}..."
                [xml]\$pub = Get-Content "\$env:PUBLISH_SETTINGS"
                \$profile = \$pub.publishData.publishProfile | Where-Object { \$_.publishMethod -eq "MSDeploy" }
                
                # Agregar validación adicional
                Write-Host "📋 Información del perfil de publicación:"
                Write-Host " - URL: \$(\$profile.publishUrl)"
                Write-Host " - Sitio: \$(\$profile.msdeploySite)"
                Write-Host " - Usuario: \$(\$profile.userName)"
                
                # Intentar una conexión de prueba
                try {
                    \$testConnection = Invoke-WebRequest -Uri "https://\$(\$profile.publishUrl)" -UseBasicParsing -TimeoutSec 10
                    Write-Host "✅ Conexión al servidor exitosa"
                } catch {
                    Write-Host "❌ No se puede conectar al servidor: \$($_.Exception.Message)"
                    exit 1
                }
                
                # Ejecutar MSBuild con parámetros adicionales para limpieza
                & "${paths.msbuild}" "ApiCrmVitalea.csproj" `
                    /p:Configuration=${CONFIGURATION} `
                    /p:DeployOnBuild=true `
                    /p:PublishProfile="\$env:PUBLISH_SETTINGS" `
                    /p:WebPublishMethod=MSDeploy `
                    /p:MsDeployServiceUrl="https://\$(\$profile.publishUrl)/msdeploy.axd" `
                    /p:DeployIisAppPath="\$(\$profile.msdeploySite)" `
                    /p:Username="\$(\$profile.userName)" `
                    /p:Password="\$(\$profile.userPWD)" `
                    /p:AllowUntrustedCertificate=true `
                    /p:VisualStudioVersion=17.0 `
                    /p:VSToolsPath="${paths.vstools}" `
                    /p:BuildProjectReferences=false `
                    /p:SkipResolveProjectReferences=true `
                    /p:TargetFrameworkVersion=v4.7.2 `
                    /p:DeleteExistingFiles=True `
                    /p:WebPublishMethod=MSDeploy `
                    /maxcpucount `
                    /verbosity:detailed  # Aumentar verbosidad para diagnóstico

                if (\$LASTEXITCODE -ne 0) { 
                    Write-Error "❌ [Deploy] Error en publicación"; 
                    exit 1 
                } else {
                    Write-Host "✅ Publicación completada exitosamente"
                    
                    # Verificación adicional: intentar acceder al sitio
                    try {
                        \$siteUrl = "https://\$(\$profile.publishUrl).replace('msdeploy', '')"
                        Write-Host "🔍 Verificando sitio en: \$siteUrl"
                        \$response = Invoke-WebRequest -Uri \$siteUrl -UseBasicParsing -TimeoutSec 30
                        Write-Host "✅ Sitio respondió correctamente - Estado: \$(\$response.StatusCode)"
                    } catch {
                        Write-Host "⚠️  El sitio no responde inmediatamente: \$($_.Exception.Message)"
                    }
                }
            """
        }
    }
}