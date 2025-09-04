def call(String url, String apiName) {
    stage("🔎 Validar ${apiName}") {
        script {
            writeFile file: 'checkApi.ps1', text: """
                try {
                    (Invoke-WebRequest -Uri '${url}' -UseBasicParsing).StatusCode
                } catch {
                    Write-Host "❌ Exception.Message: $($_.Exception.Message)"
                    if ($_.Exception.Response) {
                        $_.Exception.Response.StatusCode.value__
                    } else {
                        0
                    }
                }
            """

            def output = bat(
                script: "powershell -ExecutionPolicy Bypass -File checkApi.ps1",
                returnStdout: true
            ).trim()

            echo "📡 Output PowerShell: ${output}"

            def statusCode = output.tokenize().last().toInteger()

            echo "📡 Respuesta de ${apiName}: código ${statusCode}"

            switch (statusCode) {
                case 500..599:
                    error("❌ Error de servidor en ${apiName} (${statusCode}).")
                    break
                case 0:
                    error("❌ No se pudo obtener respuesta de la API ${apiName}")
                    break
                default:
                    if (statusCode in 400..499) {
                        echo "⚠️ Error de cliente (${statusCode}) en ${apiName}. Ignorado."
                    } else {
                        echo "✅ La API ${apiName} está operativa (${statusCode})"
                    }
            }
        }
    }
}
