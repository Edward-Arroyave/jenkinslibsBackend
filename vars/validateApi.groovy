def call(String url, String apiName) {
    stage("🔎 Validar ${apiName}") {
        echo "🌐 Validando API: ${apiName} en ${url}"

        def response = httpRequest(
            url: url,
            validResponseCodes: '100:599', // aceptamos todos, para analizarlos manualmente
            consoleLogResponseBody: true,
            timeout: 20
        )

        echo "📡 Respuesta de ${apiName}: código ${response.status}"

        if (response.status >= 500 && response.status <= 599) {
            error("❌ La API ${apiName} devolvió un error de servidor (código ${response.status})")
        } else {
            echo "✅ La API ${apiName} está operativa (código ${response.status})"
        }
    }
}
