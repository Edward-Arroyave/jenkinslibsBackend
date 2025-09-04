def call(String url, String apiName) {
    stage("🔎 Validar API ${apiName}") {
        script {
            echo """
            🚀 Iniciando validación de la API: ${apiName}
            🌍 URL objetivo: ${url}
            📤 Enviando petición HTTP...
            """

            def statusCode = bat(
                script: """powershell -Command "& {
                    try {
                        (Invoke-WebRequest -Uri '${url}' -UseBasicParsing).StatusCode
                    } catch {
                        if (\$_ -and \$_ .Exception.Response) {
                            \$_ .Exception.Response.StatusCode.value__
                        } else {
                            0
                        }
                    }
                }" """,
                returnStdout: true
            ).trim().toInteger()

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

            echo "Validación finalizada para la API: ${apiName}"
        }
    }
}
