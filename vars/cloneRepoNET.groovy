/**
 * Pipeline step para clonar un repositorio Git de forma superficial (shallow clone).
 *
 * @param config Map con configuración requerida:
 *   - branch (String): Rama del repositorio que se va a clonar.
 *   - repoPath (String): Ruta local donde se clonará el repositorio.
 *   - repoUrl (String): URL del repositorio Git.
 *
 * Requisitos:
 *   - Credenciales con ID 'GITHUB' configuradas en Jenkins para acceso al repositorio.
 *
 * Características:
 *   - Clonación superficial con profundidad 1 para acelerar el proceso.
 *   - No clona submódulos.
 */
def call(Map config) {

    // Validar que los parámetros obligatorios estén presentes
    if (!config.branch || !config.repoPath || !config.repoUrl) {
        error("❌ cloneRepo: 'branch', 'repoPath', and 'repoUrl' parameters are required.")
    }

    // Mostrar información del proceso de clonación
    echo "📦 Cloning repository (shallow clone):"
    echo "   🟢 Branch: ${config.branch}"
    echo "   📁 Path: ${config.repoPath}"
    echo "   🔗 URL: ${config.repoUrl}"

    // Cambiar al directorio destino y ejecutar la clonación
    dir(config.repoPath) {
        checkout([
            $class: 'GitSCM',
            branches: [[name: "*/${config.branch}"]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
                [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true]
            ],
            userRemoteConfigs: [[
                url: config.repoUrl,
                credentialsId: 'GITHUB' // Credenciales configuradas en Jenkins
            ]]
        ])


        // sh "git config --global --add safe.directory ${config.repoPath}"
        
        def lastCommit = sh(script: "git log -1 --pretty='%H|%an|%s'", returnStdout: true).trim()
        def (hash, author, message) = lastCommit.split("\\|")
        env.COMMIT_HASH = hash
        env.COMMIT_AUTHOR = author
        env.COMMIT_MESSAGE = message

        // echo "🔍 Último commit: ${env.COMMIT_HASH} por ${env.COMMIT_AUTHOR} - ${env.COMMIT_MESSAGE}"

    }

    // Confirmación de éxito
    echo "✅ Repository successfully shallow-cloned at: ${config.repoPath}"
}
