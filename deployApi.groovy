def call(api, configCompleto, config, CONFIGURATION) {
    try {
        dir("${configCompleto.APIS[api].REPO_PATH}") {
            def csproj = readFile(file: "${api}.csproj")

            if (csproj.contains("<TargetFrameworkVersion>v4")) {
                echo "⚙️ Proyecto ${api} detectado como .NET Framework 4.x"
                deployDotNetFramework4x(api, configCompleto, config, CONFIGURATION)
            } else {
                echo "⚙️ Proyecto ${api} detectado como .NET Core / .NET 5+"
                deployDotNetCore(api, configCompleto, config, CONFIGURATION)
            }
        }

        apisExitosas << api
        echo "🎉 DESPLIEGUE EXITOSO: ${api}"

    } catch (err) {
        echo "❌ ERROR EN DESPLIEGUE ${api}: ${err.message}"
        apisFallidas << api
        currentBuild.result = 'UNSTABLE'
    }
}
