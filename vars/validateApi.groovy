def call(String url, String apiName) {
    stage("🔎 Validar API ${apiName}") {
        script {
            echo """
            🚀 Iniciando validación de la API: ${apiName}
            🌍 URL objetivo: ${url}
            📤 Enviando petición HTTP...
            """

            // Ejecutamos PowerShell y siempre devolvemos "STATUS:<codigo>"
            def rawOutput = bat(
                script: """powershell -ExecutionPolicy Bypass -Command "& {
                    try {
                        \$resp = Invoke-WebRequest -Uri '${url}' -UseBasicParsing
                        Write-Output ('STATUS:' + \$resp.StatusCode)
                    } catch {
                        if (\$_ -and \$_.Exception.Response) {
                            Write-Output ('STATUS:' + \$_.Exception.Response.StatusCode.value__)
                        } else {
                            Write-Output 'STATUS:0'
                        }
                    }
                }" """,
                returnStdout: true
            ).trim()

            echo "📡 Output PowerShell: ${rawOutput}"

            // Extraer solo el número después de "STATUS:"
            def statusCode = rawOutput.readLines()
                                      .find { it.startsWith("STATUS:") }
                                      ?.replace("STATUS:", "")
                                      ?.trim()
                                      ?.toInteger() ?: 0

            echo "📡 Respuesta de ${apiName}: código ${statusCode}"
            echo "🔎 Analizando código de estado..."

            switch (statusCode) {
                case 500..599:
                    error("❌ Error de servidor en ${apiName} (${statusCode}). Posible fallo en el despliegue.")
                    break
                case 400..499:
                    echo "⚠️ Error de cliente (${statusCode}) en ${apiName}. Ignorado (puede ser CORS o autenticación)."
                    break
                case 0:
                    error("❌ No se pudo obtener respuesta de la API ${apiName}")
                    break
                default:
                    echo "✅ La API ${apiName} está operativa (${statusCode})"
            }

            echo "✅ Validación finalizada para la API: ${apiName}"
        }
    }
}
