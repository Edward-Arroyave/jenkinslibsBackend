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

    // Logs mejorados para Ocean/consola
    echo ""
    echo "📊 =============================== NOTIFICACIÓN TEAMS ==============================="
    echo "${logEmoji} Estado del Build: ${statusText}"
    echo "👤 Triggered by: ${env.BUILD_USER_ID ?: 'N/A'}"
    echo "🌍 Environment: ${config.ENVIRONMENT ?: 'N/A'}"
    echo "⏱️  Duration: ${durationText}"
    echo "🔢 Build: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    
    if (env.COMMIT_AUTHOR) {
        echo "👨‍💻 Commit Author: ${env.COMMIT_AUTHOR}"
    }
    if (env.COMMIT_MESSAGE) {
        echo "📝 Commit Message: ${env.COMMIT_MESSAGE.take(80)}${env.COMMIT_MESSAGE.length() > 80 ? '...' : ''}"
    }
    if (env.COMMIT_HASH) {
        echo "🔗 Commit Hash: ${env.COMMIT_HASH.take(8)}"
    }
    
    echo "✅ APIs Exitosas: ${config.APIS_SUCCESSFUL ?: 'Ninguna'}"
    echo "❌ APIs Fallidas: ${config.APIS_FAILURE ?: 'Ninguna'}"
    echo "================================================================================"
    echo ""

    // Enviar notificación a Teams
    wrap([$class: 'BuildUser']) {
        try {
            office365ConnectorSend(
                status: status,
                message: "${emoji} ${statusText}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                adaptiveCards: true,
                color: color,
                factDefinitions: [
                    [name: "Build triggered by", template: "${env.BUILD_USER_ID}"],
                    [name: "Environment", template: "${config.ENVIRONMENT}"],
                    [name: "Commit Author", template: "${env.COMMIT_AUTHOR}"],
                    [name: "Commit Message", template: "${env.COMMIT_MESSAGE}"],
                    [name: "Commit Hash", template: "${env.COMMIT_HASH}"],
                    [name: "Duration", template: durationText],
                    [name: "APIS_SUCCESSFUL", template: "✅${config.APIS_SUCCESSFUL}"],
                    [name: "APIS_FAILURE", template: "❌ ${config.APIS_FAILURE}"],
                ]
            )
            echo "📢 ✅ Notificación enviada exitosamente a Teams"
        } catch (Exception e) {
            echo "❌ ⚠️  Error enviando notificación a Teams: ${e.message}"
            echo "📋 Se mostró la información en consola igualmente"
        }
    }

    // Log final con emoji según estado
    echo "${logEmoji} ${statusText} - Duración: ${durationText}"
    echo ""
}