def call(String url, String apiName) {
    stage("🔎 Validar ${apiName}") {
        script {
            // Usar comillas simples triples y concatenación para la URL
            writeFile file: 'checkApi.ps1', text: '''
                try {
                    $resp = Invoke-WebRequest -Uri ''' + "'${url}'" + ''' -UseBasicParsing
                    Write-Output ('STATUS:' + $resp.StatusCode)
                } catch {
                    Write-Output "❌ Exception.Message: $($_.Exception.Message)"
                    if ($_.Exception.Response) {
                        Write-Output ('STATUS:' + $_.Exception.Response.StatusCode.value__)
                    } else {
                        Write-Output 'STATUS:0'
                    }
                }
            '''.stripIndent()

            def output = bat(
                script: "powershell -ExecutionPolicy Bypass -File checkApi.ps1",
                returnStdout: true
            ).trim()

            echo "📡 Output PowerShell: ${output}"

            def statusLine = output.readLines().find { it.startsWith("STATUS:") }
            def statusCode = (statusLine ? statusLine.replace("STATUS:", "").trim() : "0").toInteger()

            echo "📡 Respuesta de ${apiName}: código ${statusCode}"

            if (statusCode >= 500) {
                error("❌ Error de servidor en ${apiName} (${statusCode}).")
            } else if (statusCode == 0) {
                error("❌ No se pudo conectar a ${apiName}")
            } else if (statusCode >= 400) {
                echo "⚠️ Error de cliente (${statusCode}) en ${apiName}. Ignorado."
            } else {
                echo "✅ API ${apiName} respondió correctamente (${statusCode})"
            }
        }
    }
}