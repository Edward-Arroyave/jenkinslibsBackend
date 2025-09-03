def call(Map config) {

    // Obtener duración real del build en milisegundos
    def durationMillis = currentBuild.duration ?: (currentBuild.getTimeInMillis() - currentBuild.getStartTimeInMillis())

    // Convertir a H.M.S
    def totalSeconds = durationMillis / 1000.0
    def hours = Math.floor(totalSeconds / 3600).toInteger()
    def minutes = Math.floor((totalSeconds - (hours * 3600)) / 60).toInteger()
    def seconds = totalSeconds - (hours * 3600) - (minutes * 60)

    def durationText = ""
    if (hours > 0) { durationText += "${hours}h " }
    if (minutes > 0) { durationText += "${minutes}m " }
    durationText += String.format("%.1f", seconds) + "s"

    def status = currentBuild.currentResult ?: "FAILURE"
    
    // Mapa de estados base
    def statusMap = [
        "SUCCESS" : [color: "00FF00", emoji: "✅", statusText: "Build Succeeded", logEmoji: "🎉"],
        "UNSTABLE": [color: "FFFF00", emoji: "⚠️", statusText: "Build Unstable", logEmoji: "⚡"],
        "ABORTED" : [color: "FFA500", emoji: "⏹️", statusText: "Build Aborted", logEmoji: "⏹️"],
        "FAILURE" : [color: "FF0000", emoji: "❌", statusText: "Build Failed", logEmoji: "💥"]
    ]

    // Valores por defecto según el estado actual
    def (color, emoji, statusText, logEmoji) = statusMap[status]?.values() ?: statusMap["FAILURE"].values()

    // Reglas adicionales según config
    if (config.APIS_FAILURE) {
        if (!config.APIS_SUCCESSFUL) {
            (color, emoji, statusText, logEmoji) = statusMap["UNSTABLE"].values()
            status = "UNSTABLE"
        } else {
            (color, emoji, statusText, logEmoji) = statusMap["FAILURE"].values()
            status = "FAILURE"
        }
    }

    // Logs empresariales para Ocean/consola
    echo ""
    echo "📊 =========================== REPORTE DE EJECUCIÓN ==========================="
    echo "📌 Estado del Proceso: ${statusText}"
    echo "👤 Usuario que ejecutó: ${env.BUILD_USER_ID ?: 'No disponible'}"
    echo "🌍 Entorno: ${config.ENVIRONMENT ?: 'No definido'}"
    echo "⏱️ Duración total: ${durationText}"
    echo "🔢 Proceso: ${env.JOB_NAME} #${env.BUILD_NUMBER}"

    if (env.COMMIT_AUTHOR) {
        echo "👨‍💻 Autor del Commit: ${env.COMMIT_AUTHOR}"
    }
    if (env.COMMIT_MESSAGE) {
        echo "📝 Mensaje del Commit: ${env.COMMIT_MESSAGE.take(80)}${env.COMMIT_MESSAGE.length() > 80 ? '...' : ''}"
    }
    if (env.COMMIT_HASH) {
        echo "🔗 Hash de Commit: ${env.COMMIT_HASH.take(8)}"
    }

    echo "✅ APIs procesadas con éxito: ${config.APIS_SUCCESSFUL ?: 'Ninguna'}"
    echo "❌ APIs con errores: ${config.APIS_FAILURE ?: 'Ninguna'}"
    echo "============================================================================="
    echo ""

    // Enviar notificación a Teams 
    wrap([$class: 'BuildUser']) {
        try {
            // Texto plano para fallback
            def apisSuccess = config.APIS_SUCCESSFUL ?: "Ninguna"
            def apisFailure = config.APIS_FAILURE ?: "Ninguna"

            office365ConnectorSend(
                status: status,
                message: "👋 Buen día ingenieros.  \n\nLes informamos el estado del proceso de despliegue ejecutado.",
                color: color,
                adaptiveCards: [
                    [
                        "@type": "MessageCard",
                        "@context": "http://schema.org/extensions",
                        "summary": "Reporte de despliegue",
                        "themeColor": color,
                        "sections": [[
                            "activityTitle": "${emoji} Estado: ${statusText}",
                            "facts": [
                                ["name": "Usuario ejecutor", "value": "${env.BUILD_USER_ID}"],
                                ["name": "Entorno", "value": "${config.ENVIRONMENT}"],
                                ["name": "Autor del Commit", "value": "${env.COMMIT_AUTHOR}"],
                                ["name": "Mensaje del Commit", "value": "${env.COMMIT_MESSAGE}"],
                                ["name": "Hash del Commit", "value": "${env.COMMIT_HASH}"],
                                ["name": "Duración", "value": durationText]
                            ],
                            "markdown": true
                        ]],
                        "potentialAction": []
                    ],
                    [
                        "type": "AdaptiveCard",
                        "version": "1.2",
                        "body": [
                            [
                                "type": "TextBlock",
                                "text": "✅ APIs Exitosas: ${apisSuccess}",
                                "color": "good",
                                "wrap": true
                            ],
                            [
                                "type": "TextBlock",
                                "text": "❌ APIs con Errores: ${apisFailure}",
                                "color": "attention",
                                "wrap": true
                            ]
                        ]
                    ]
                ]
            )
            echo "📢 Notificación enviada a Microsoft Teams de manera exitosa."
        } catch (Exception e) {
            echo "⚠️ No fue posible enviar la notificación a Teams: ${e.message}"
            echo "📋 La información fue presentada en consola."
        }
    }


    // Log final formal
    echo "${logEmoji} Estado Final: ${statusText} | Duración: ${durationText}"
    echo ""
}