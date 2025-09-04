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

    echo "Full name: $BUILD_USER"
    echo "First name: $BUILD_USER_FIRST_NAME"
    echo "Last name: $BUILD_USER_LAST_NAME"
    echo "User id: $BUILD_USER_ID"


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
            office365ConnectorSend(
                status: status,
                message: """
                Buen día ingenieros.  
                Les informamos el estado del proceso de despliegue ejecutado:  
                Proceso: ${env.JOB_NAME} #${env.BUILD_NUMBER}   
                Agradecemos su atención y quedamos atentos a observaciones o comentarios adicionales. 
                """,
                adaptiveCards: true,
                color: color,
                factDefinitions: [
                    [name: "Usuario ejecutor", template: "${env.BUILD_USER_ID}"],
                    [name: "Entorno", template: "${config.ENVIRONMENT}"],
                    [name: "Autor del Commit", template: "${env.COMMIT_AUTHOR}"],
                    [name: "Mensaje del Commit", template: "${env.COMMIT_MESSAGE}"],
                    [name: "Hash del Commit", template: "${env.COMMIT_HASH}"],
                    [name: "Duración", template: durationText],
                    [name: "APIs Exitosas", template: "${config.APIS_SUCCESSFUL}"],
                    [name: "APIs con Errores", template: "${config.APIS_FAILURE}"],
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