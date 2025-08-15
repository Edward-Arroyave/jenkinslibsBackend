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

        // safe.directory
        bat "git config --global --add safe.directory \"${config.repoPath}\""

        // Obtener último commit en Windows usando bat
        def lastCommit = bat(
            script: 'for /f "tokens=1,2,* delims=|" %%A in (\'git log -1 --pretty=^"%%H|%%an|%%s^"\') do @echo %%A|%%B|%%C',
            returnStdout: true
        ).trim()
        lastCommit = lastCommit.replaceAll("\r","") // limpiar retornos de carro

        def (hash, author, message) = lastCommit.split("\\|")
        env.COMMIT_HASH = hash
        env.COMMIT_AUTHOR = author
        env.COMMIT_MESSAGE = message

        echo "🔍 Último commit: ${env.COMMIT_HASH} por ${env.COMMIT_AUTHOR} - ${env.COMMIT_MESSAGE}"
    }

    echo "✅ Repository successfully shallow-cloned at: ${config.repoPath}"
}
