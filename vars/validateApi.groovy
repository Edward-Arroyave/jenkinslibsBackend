def call(String url, String apiName) {
    stage("🔎 Validar API ${apiName}") {
        script {
            echo "🚀 Iniciando validación de la API: ${apiName}"
            echo "🌍 URL objetivo: ${url}"

            echo "📤 Enviando petición HTTP..."
            def statusCode = bat(
                script: """powershell -Command "(Invoke-WebRequest -Uri '${url}' -UseBasicParsing).StatusCode" """,
                returnStdout: true
            ).trim()

            echo "📥 Petición finalizada."
            echo "📡 Respuesta recibida de ${apiName}: código ${statusCode}"

            int code = statusCode.toInteger()

            echo "🔎 Analizando código de estado..."

            if (code >= 500 && code <= 599) {
                error("❌ La API ${apiName} devolvió un error de servidor (${code})")
            } else if (code >= 400 && code <= 499) {
                echo "⚠️ La API ${apiName} devolvió un error de cliente (${code}), no se considera error de despliegue"
            } else {
                echo "✅ La API ${apiName} está operativa (${code})"
            }

            echo "🏁 Validación finalizada para la API: ${apiName}"
        }
    }
}
