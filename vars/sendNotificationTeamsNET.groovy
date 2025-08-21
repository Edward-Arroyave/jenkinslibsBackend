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
        "SUCCESS" : [color: "00FF00", emoji: "✅", statusText: "Build Succeeded"],
        "UNSTABLE": [color: "FFFF00", emoji: "⚠️", statusText: "Build Unstable"],
        "ABORTED" : [color: "FFA500", emoji: "⏹️", statusText: "Build Aborted"],
        "FAILURE" : [color: "FF0000", emoji: "❌", statusText: "Build Failed"]
    ]

    // Valores por defecto según el estado actual
    def (color, emoji, statusText) = statusMap[status]?.values() ?: statusMap["FAILURE"].values()

    // Reglas adicionales según config
    if (config.APIS_FAILURE) {
        if (!config.APIS_SUCCESSFUL) {
            (color, emoji, statusText) = statusMap["FAILURE"].values()
        } else {
            (color, emoji, statusText) = statusMap["UNSTABLE"].values()
        }
    }

    // Enviar notificación a Teams
    wrap([$class: 'BuildUser']) {
        office365ConnectorSend(
            status: status,
            message: "${emoji} ${statusText}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            adaptiveCards: true,
            color: color,
            factDefinitions: [
                [name: "Build triggered by", template: "${env.BUILD_USER_ID}"],
                [name: "Commit Author", template: "${env.COMMIT_AUTHOR}"],
                [name: "Commit Message", template: "${env.COMMIT_MESSAGE}"],
                [name: "Commit Hash", template: "${env.COMMIT_HASH}"],
                [name: "Duration", template: durationText],
                [name: "APIS_SUCCESSFUL", template: "${config.APIS_SUCCESSFUL}"],
                [name: "APIS_FAILURE", template: "${config.APIS_FAILURE}"],

            ]
        )
    }


    echo "📢 Notificación enviada: ${statusText} (${durationText})"
}
