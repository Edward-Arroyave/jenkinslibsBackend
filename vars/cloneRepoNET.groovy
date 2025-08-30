def call(Map config) {
    if (!config.branch || !config.repoPath || !config.repoUrl) {
        error("❌ cloneRepo: 'branch', 'repoPath', and 'repoUrl' parameters are required.")
    }

    echo "📦 Cloning repository (shallow clone):"
    echo "   🟢 Branch: ${config.branch}"
    echo "   📁 Path: ${config.repoPath}"
    echo "   🔗 URL: ${config.repoUrl}"

    // Crear directorio si no existe
    bat """
        @echo off
        if not exist "${config.repoPath}" (
            mkdir "${config.repoPath}"
        )
    """

    // Función para limpiar archivos de bloqueo
    def cleanLockFiles = { repoPath ->
        bat """
            @echo off
            echo 🧹 Cleaning lock files in: ${repoPath}
            cd /d "${repoPath}"
            
            :: Eliminar archivos .lock en todas las subcarpetas relevantes
            for /r . %%f in (*.lock) do (
                echo Eliminando: %%f
                del /f /q "%%f" 2>nul
            )
            
            :: Eliminar específicamente en .git/refs/remotes/origin/
            if exist ".git" (
                cd .git
                if exist "refs" (
                    cd refs
                    if exist "remotes" (
                        cd remotes
                        if exist "origin" (
                            cd origin
                            for /r . %%f in (*.lock) do (
                                echo Eliminando lock file en origin: %%f
                                del /f /q "%%f" 2>nul
                            )
                        )
                    )
                )
            )
        """
    }

    // Limpieza preventiva antes de clonar
    cleanLockFiles(config.repoPath)

    dir(config.repoPath) {
        def maxRetries = 3
        def retryCount = 0
        def success = false
        
        while (!success && retryCount < maxRetries) {
            try {
                retryCount++
                echo "🔄 Attempt ${retryCount} of ${maxRetries}"
                
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${config.branch}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                        [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true],
                        [$class: 'CleanBeforeCheckout']  // Limpiar antes del checkout
                    ],
                    userRemoteConfigs: [[
                        url: config.repoUrl,
                        credentialsId: 'GITHUB'
                    ]]
                ])

                // Configurar safe.directory
                bat "git config --global --add safe.directory \"${config.repoPath.replace('/', '\\')}\""

                // Obtener último commit
                def lastCommit = bat(
                    script: '@for /f "delims=" %%a in (\'git log -1 --pretty^=format:"%%H|%%an|%%s"\') do @echo %%a',
                    returnStdout: true
                ).trim()

                lastCommit = lastCommit.replaceAll("[\\r\\n]+", "")
                def parts = lastCommit.split("\\|")
                env.COMMIT_HASH = parts[0]
                env.COMMIT_AUTHOR = parts.length > 1 ? parts[1] : ""
                env.COMMIT_MESSAGE = parts.length > 2 ? parts[2] : ""

                echo "🔍 Último commit: ${env.COMMIT_HASH} por ${env.COMMIT_AUTHOR} - ${env.COMMIT_MESSAGE}"
                success = true

            } catch (Exception e) {
                echo "❌ Error en intento ${retryCount}: ${e.message}"
                
                if (retryCount < maxRetries) {
                    echo "🔄 Reintentando después de limpiar archivos de bloqueo..."
                    cleanLockFiles(config.repoPath)
                    sleep(time: 5, unit: 'SECONDS') // Esperar antes de reintentar
                } else {
                    error("💥 Fallo después de ${maxRetries} intentos: ${e.message}")
                }
            }
        }
    }

    echo "✅ Repository successfully shallow-cloned at: ${config.repoPath}"
}