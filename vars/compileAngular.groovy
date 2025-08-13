/**
 * Pipeline step para instalar dependencias de Node.js y compilar una aplicación Angular.
 *
 * @param config Map con configuración esperada:
 *   - repoPath (String): Ruta local donde se encuentra el código fuente de Angular.
 *   - distPath (String): Ruta donde se espera que se genere la carpeta compilada (build output).
 *
 * Pasos realizados:
 *   1. Valida que los parámetros obligatorios existan.
 *   2. Ejecuta 'npm install' para instalar dependencias en repoPath.
 *   3. Ejecuta el comando de compilación Angular con optimización para producción.
 *   4. Verifica que la carpeta de compilación distPath exista.
 */
def call(Map config) {
    // Validar parámetros obligatorios
    if (!config.repoPath) error "Falta el parámetro obligatorio: repoPath"
    if (!config.distPath) error "Falta el parámetro obligatorio: distPath"

    def repoPath = config.repoPath
    def distPath = config.distPath

    // Instalar dependencias de Node.js usando npm
    echo "📥 Instalando dependencias..."
    dir(repoPath) {
        sh 'npm install -f'
    }
    echo "✅ Dependencias instaladas"

    // Compilar la aplicación Angular con flags para producción y optimización
    echo "⚙️ Compilando Angular..."
    dir(repoPath) {
        sh 'node --max-old-space-size=9096 ./node_modules/.bin/ng build --aot --configuration production --optimization'
    }
    echo "✅ Compilación completada"

    // Verificar que la carpeta de compilación exista para validar éxito del build
    echo "📂 Verificando carpeta: ${distPath}"
    if (!fileExists(distPath)) {
        error "❌ ERROR: No se encontró la carpeta compilada en ${distPath}"
    }
    echo "✅ Carpeta compilada encontrada"
}
