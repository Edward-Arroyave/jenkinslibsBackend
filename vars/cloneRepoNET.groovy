def call(Map config) {

    if (!config.branch || !config.repoPath || !config.repoUrl) {
        error("❌ cloneRepo: 'branch', 'repoPath', and 'repoUrl' parameters are required.")
    }

    echo "📦 Cloning repository (shallow clone):"
    echo "   🟢 Branch: ${config.branch}"
    echo "   📁 Path: ${config.repoPath}"
    echo "   🔗 URL: ${config.repoUrl}"

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
                credentialsId: 'GITHUB'
            ]]
        ])

        // Configurar safe.directory correctamente para Windows
        bat "git config --global --add safe.directory \"${config.repoPath}\""

        // Obtener último commit en Windows CMD de forma segura
        def lastCommit = bat(
            script: """for /f "tokens=1,2,* delims=%%" %%A in ('git log -1 --pretty=format:"%%H%%|%%an%%|%%s"') do @echo %%A|%%B|%%C""",
            returnStdout: true
        ).trim()

        // Limpiar retornos de carro y saltos de línea
        lastCommit = lastCommit.replaceAll("[\\r\\n]+", "")
        def parts = lastCommit.split("\\|")
        def hash = parts[0]
        def author = parts.length > 1 ? parts[1] : ""
        def message = parts.length > 2 ? parts[2] : ""

        env.COMMIT_HASH = hash
        env.COMMIT_AUTHOR = author
        env.COMMIT_MESSAGE = message

        echo "🔍 Último commit: ${env.COMMIT_HASH} por ${env.COMMIT_AUTHOR} - ${env.COMMIT_MESSAGE}"
    }

    echo "✅ Repository successfully shallow-cloned at: ${config.repoPath}"
}
