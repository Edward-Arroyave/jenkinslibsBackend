def call(String url, String apiName) {
    stage("🔎 Validar ${apiName}") {
        script {
            writeFile file: 'checkApi.ps1', text: """
                try {
                    \$resp = Invoke-WebRequest -Uri '${url}' -UseBasicParsing
                    Write-Output ('STATUS:' + \$resp.StatusCode)
                } catch {
                    Write-Output "❌ Exception.Message: $($_.Exception.Message)"
                    if (\$_.Exception.Response) {
                        Write-Output ('STATUS:' + \$_.Exception.Response.StatusCode.value__)
                    } else {
                        Write-Output 'STATUS:0'
                    }
                }
            """

            def output = bat(
                script: "powershell -ExecutionPolicy Bypass -File checkApi.ps1",
                returnStdout: true
            ).trim()

            echo "📡 Output PowerShell: ${output}"

            // Buscar la línea que empieza con STATUS:
            def statusCode = output.readLines()
                                   .find { it.startsWith("STATUS:") }
                                   ?.replace("STATUS:", "")
                                   ?.trim()
                                   ?.toInteger() ?: 0

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
