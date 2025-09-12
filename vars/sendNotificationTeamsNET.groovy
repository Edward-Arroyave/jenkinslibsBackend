def call(Map config) {
    // Calcular duración
    def durationMillis = currentBuild.duration ?: (currentBuild.getTimeInMillis() - currentBuild.getStartTimeInMillis())
    def totalSeconds = durationMillis / 1000.0
    def hours = Math.floor(totalSeconds / 3600).toInteger()
    def minutes = Math.floor((totalSeconds - (hours * 3600)) / 60).toInteger()
    def seconds = totalSeconds - (hours * 3600) - (minutes * 60)

    def durationText = ""
    if (hours > 0) { durationText += "${hours}h " }
    if (minutes > 0) { durationText += "${minutes}m " }
    durationText += String.format("%.1f", seconds) + "s"

    def status = currentBuild.currentResult ?: "FAILURE"

    // Estados
    def statusMap = [
        "SUCCESS" : [color: "00FF00", emoji: "✅", statusText: "Build Succeeded", logEmoji: "🎉"],
        "UNSTABLE": [color: "FFFF00", emoji: "⚠️", statusText: "Build Unstable", logEmoji: "⚡"],
        "ABORTED" : [color: "FFA500", emoji: "⏹️", statusText: "Build Aborted", logEmoji: "⏹️"],
        "FAILURE" : [color: "FF0000", emoji: "❌", statusText: "Build Failed", logEmoji: "💥"]
    ]

    def (color, emoji, statusText, logEmoji) = statusMap[status]?.values() ?: statusMap["FAILURE"].values()

    if (config.APIS_FAILURE) {
        if (!config.APIS_SUCCESSFUL) {
            (color, emoji, statusText, logEmoji) = statusMap["UNSTABLE"].values()
            status = "UNSTABLE"
        } else {
            (color, emoji, statusText, logEmoji) = statusMap["FAILURE"].values()
            status = "FAILURE"
        }
    }

    try {
        def sendNotification = { webhookUrl = "" ->
            office365ConnectorSend(
                status: status,
                webhookUrl: webhookUrl,
                message: """
                Buen día ingenieros.  
                Les informamos el estado del proceso de despliegue ejecutado:  
                Proceso: **${env.JOB_NAME} #${env.BUILD_NUMBER}**  
                Agradecemos su atención y quedamos atentos a observaciones o comentarios adicionales. 
                """,
                adaptiveCards: true,
                color: color,
                factDefinitions: [
                    [name: "📌 Estado Final", template: "**${statusText} ${emoji}**"],
                    [name: "👤 Usuario ejecutor", template: "_${env.BUILD_USER}_"],
                    [name: "📧 Usuario correo", template: "_${env.BUILD_USER_EMAIL}_"],
                    [name: "🌍 Entorno", template: "**${config.ENVIRONMENT}**"],
                    [name: "👨‍💻 Autor del Commit", template: "${env.COMMIT_AUTHOR}"],
                    [name: "📝 Commit", template: "${env.COMMIT_MESSAGE}"],
                    [name: "🔗 Hash del Commit", template: "`${env.COMMIT_HASH} `"],
                    [name: "⏱️ Duración", template: "` ${durationText} `"],
                    [name: "✅ APIs Exitosas", template: "**${config.APIS_SUCCESSFUL ?: 'Ninguna'}**"],
                    [name: "❌ APIs con Errores", template: "**${config.APIS_FAILURE ?: 'Ninguna'}**"],
                ]
            )
        }

        if (config.PRODUCT == "AGENDAMIENTO") {
            withCredentials([string(credentialsId: 'WEBHOOK_HEALTHBOOK', variable: 'WEBHOOK_URL')]) {
                sendNotification(WEBHOOK_URL)
            }
        } else {
            sendNotification() // sin webhook, se usa el default si existe
        }

        echo "📢 Notificación enviada a Microsoft Teams de manera exitosa."
    } catch (Exception e) {
        echo "⚠️ No fue posible enviar la notificación a Teams: ${e.message}"
        echo "📋 La información fue presentada en consola."
    }

    echo "${logEmoji} Estado Final: ${statusText} | Duración: ${durationText}"
}
