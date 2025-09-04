def call(String url, String apiName) {
    echo "🌐 Validando API: ${apiName} en ${url}"

    def response = httpRequest(
        url: url,
        validResponseCodes: '100:399',
        consoleLogResponseBody: true,
        timeout: 20
    )

    if (response.status >= 400) {
        error("❌ La API ${apiName} respondió con código ${response.status}")
    }

    echo "✅ La API ${apiName} está saludable (código ${response.status})"
}
