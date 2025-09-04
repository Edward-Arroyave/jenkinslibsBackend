def call(String url, String apiName) {
        stage("🔎 Validar ${apiName}") {
            steps {
                script {
                    writeFile file: 'checkApi.ps1', text: '''
                        try {
                            (Invoke-WebRequest -Uri $env:API_URL -UseBasicParsing).StatusCode
                        } catch {
                            Write-Host "❌ Exception.Message: $($_.Exception.Message)"
                            if ($_.Exception.Response) {
                                $_.Exception.Response.StatusCode.value__
                            } else {
                                0
                            }
                        }
                    '''

                    def output = bat(
                        script: "powershell -ExecutionPolicy Bypass -File checkApi.ps1",
                        returnStdout: true
                    ).trim()

                    echo "📡 Output PowerShell: ${output}"

                    def statusCode = output.tokenize().last().toInteger()

                    echo "📡 Respuesta de ${env.API_NAME}: código ${statusCode}"

                    switch (statusCode) {
                        case 500..599:
                            error("❌ Error de servidor en ${env.API_NAME} (${statusCode}).")
                        case 400..499:
                            echo "⚠️ Error de cliente (${statusCode}) en ${env.API_NAME}. Ignorado."
                        case 0:
                            error("❌ No se pudo obtener respuesta de la API ${env.API_NAME}")
                        default:
                            echo "✅ La API ${env.API_NAME} está operativa (${statusCode})"
                    }
                }
            }
        }
}
