param(
    [switch]$SkipTests,
    [switch]$SkipPackage
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$lib = Join-Path $root 'lib\flatlaf-3.6.1.jar'
$classes = Join-Path $root 'build\classes'
$testClasses = Join-Path $root 'build\test-classes'
$fat = Join-Path $root 'build\fat'
$jarFile = Join-Path $root 'build\FileSender.jar'
$inputDir = Join-Path $root 'build\app-input'
$distDir = Join-Path $root 'dist'
$appDir = Join-Path $distDir 'FileSender'

function Resolve-JdkTool([string]$name) {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\$name.exe"
        if (Test-Path -LiteralPath $candidate) { return $candidate }
        $candidate = Join-Path $env:JAVA_HOME "bin\$name"
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    $command = Get-Command $name -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    throw "No se encuentra $name. Instala JDK 17 y configura JAVA_HOME."
}

$javac = Resolve-JdkTool 'javac'
$jar = Resolve-JdkTool 'jar'
$java = Resolve-JdkTool 'java'
$jpackage = Resolve-JdkTool 'jpackage'

Write-Host 'Limpiando...' -ForegroundColor Cyan
Remove-Item -LiteralPath $classes, $testClasses, $fat -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classes, $testClasses, $fat, $inputDir | Out-Null

Write-Host 'Compilando...' -ForegroundColor Cyan
$sources = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter '*.java' | ForEach-Object FullName
& $javac --release 17 -encoding UTF-8 -cp $lib -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación.' }

Copy-Item (Join-Path $root 'resources\icon.png') (Join-Path $classes 'icon.png') -Force
Copy-Item -Path (Join-Path $classes '*') -Destination $fat -Recurse -Force
Push-Location $fat
& $jar xf $lib
Pop-Location
Remove-Item (Join-Path $fat 'META-INF\MANIFEST.MF') -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $fat 'module-info.class') -Force -ErrorAction SilentlyContinue

& $jar --create --file $jarFile --main-class com.filesender.desktop.Main -C $fat .
if ($LASTEXITCODE -ne 0) { throw 'Falló el empaquetado JAR.' }
Write-Host ("JAR: {0:N0} KB" -f ((Get-Item $jarFile).Length / 1KB)) -ForegroundColor DarkGray

if (-not $SkipTests) {
    Write-Host 'Ejecutando pruebas...' -ForegroundColor Cyan
    $tests = Get-ChildItem (Join-Path $root 'test') -Recurse -Filter '*.java' | ForEach-Object FullName
    & $javac --release 17 -encoding UTF-8 -cp "$classes;$lib" -d $testClasses $tests
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de pruebas.' }
    & $java -cp "$testClasses;$classes;$lib" com.filesender.desktop.SelfTest
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas.' }
}

if ($SkipPackage) {
    Write-Host 'Empaquetado omitido (-SkipPackage).' -ForegroundColor Yellow
    exit 0
}

Write-Host 'Creando aplicación portátil...' -ForegroundColor Cyan
Remove-Item -LiteralPath $appDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $distDir | Out-Null
Remove-Item -Path (Join-Path $inputDir '*') -Force -ErrorAction SilentlyContinue
Copy-Item $jarFile (Join-Path $inputDir 'FileSender.jar') -Force

& $jpackage `
    --type app-image `
    --name FileSender `
    --app-version 1.0 `
    --vendor FileSender `
    --description "Enviar y recibir archivos por WiFi" `
    --input $inputDir `
    --main-jar FileSender.jar `
    --main-class com.filesender.desktop.Main `
    --icon (Join-Path $root 'FileSender.ico') `
    --dest $distDir `
    --java-options "-Dfile.encoding=UTF-8" `
    --java-options "-Dawt.useSystemAAFontSettings=on" `
    --java-options "-Dswing.aatext=true" `
    --java-options "-Xmx512m"
if ($LASTEXITCODE -ne 0) { throw 'Falló jpackage.' }

Copy-Item (Join-Path $root 'README-WINDOWS.txt') $appDir -Force
Copy-Item (Join-Path $root 'THIRD-PARTY-LICENSES.txt') $appDir -Force
Copy-Item (Join-Path $root 'PermitirFirewall.ps1') $appDir -Force
Copy-Item (Join-Path $root 'PermitirFirewall.bat') $appDir -Force
Copy-Item (Join-Path $root 'QuitarFirewall.ps1') $appDir -Force
Copy-Item (Join-Path $root 'QuitarFirewall.bat') $appDir -Force

$zip = Join-Path $distDir 'FileSender-Windows-1.0.zip'
Remove-Item -LiteralPath $zip -Force -ErrorAction SilentlyContinue
Write-Host 'Comprimiendo versión portátil...' -ForegroundColor Cyan
Compress-Archive -Path (Join-Path $appDir '*') -DestinationPath $zip -CompressionLevel Optimal
$repoDist = Join-Path (Split-Path $root -Parent) 'dist'
New-Item -ItemType Directory -Force -Path $repoDist | Out-Null
Copy-Item $zip (Join-Path $repoDist 'FileSender-Windows-1.0.zip') -Force
$hash = (Get-FileHash -Algorithm SHA256 (Join-Path $repoDist 'FileSender-Windows-1.0.zip')).Hash.ToLowerInvariant()
"$hash  FileSender-Windows-1.0.zip" | Set-Content -Encoding ascii (Join-Path $repoDist 'FileSender-Windows-1.0.zip.sha256')

Write-Host 'Creando instalador MSI...' -ForegroundColor Cyan
$wix = Join-Path $root 'tools\wix314\candle.exe'
if (-not (Test-Path -LiteralPath $wix)) {
    $wixZip = Join-Path $root 'tools\wix314-binaries.zip'
    New-Item -ItemType Directory -Force -Path (Split-Path $wixZip -Parent) | Out-Null
    if (-not (Test-Path -LiteralPath $wixZip)) {
        Write-Host 'Descargando WiX Toolset...' -ForegroundColor DarkGray
        & curl.exe -L --fail --max-time 180 -o $wixZip `
            'https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip'
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo descargar WiX Toolset.' }
    }
    Expand-Archive -LiteralPath $wixZip -DestinationPath (Join-Path $root 'tools\wix314') -Force
}

$installerOut = Join-Path $distDir 'installer'
New-Item -ItemType Directory -Force -Path $installerOut | Out-Null
Remove-Item (Join-Path $installerOut 'FileSender-1.0.msi') -Force -ErrorAction SilentlyContinue
$wixTemp = Join-Path $root 'build\wix-temp'
New-Item -ItemType Directory -Force -Path $wixTemp | Out-Null
$oldPath = $env:PATH
$oldTemp = $env:TEMP
$oldTmp = $env:TMP
$env:PATH = "$(Join-Path $root 'tools\wix314');$env:PATH"
$env:TEMP = $wixTemp
$env:TMP = $wixTemp
$installerExit = 1
try {
    & $jpackage `
        --type msi `
        --name FileSender `
        --app-version 1.0 `
        --vendor FileSender `
        --description "Enviar y recibir archivos por WiFi" `
        --app-image $appDir `
        --dest $installerOut `
        --icon (Join-Path $root 'FileSender.ico') `
        --win-dir-chooser `
        --win-shortcut `
        --win-menu `
        --win-menu-group FileSender `
        --win-per-user-install `
        --win-upgrade-uuid "6f1c2d9a-6c7b-4f2e-9d0a-2b5c3e4f5a61"
    $installerExit = $LASTEXITCODE
} finally {
    $env:PATH = $oldPath
    $env:TEMP = $oldTemp
    $env:TMP = $oldTmp
}
if ($installerExit -ne 0) { throw 'Falló la creación del instalador MSI.' }
$msi = Join-Path $installerOut 'FileSender-1.0.msi'
Copy-Item $msi (Join-Path $repoDist 'FileSender-Windows-1.0-Setup.msi') -Force
$msiHash = (Get-FileHash -Algorithm SHA256 (Join-Path $repoDist 'FileSender-Windows-1.0-Setup.msi')).Hash.ToLowerInvariant()
"$msiHash  FileSender-Windows-1.0-Setup.msi" | Set-Content -Encoding ascii (Join-Path $repoDist 'FileSender-Windows-1.0-Setup.msi.sha256')
Remove-Item (Join-Path $repoDist 'FileSender-Windows-1.0-Setup.exe') -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $repoDist 'FileSender-Windows-1.0-Setup.exe.sha256') -Force -ErrorAction SilentlyContinue

Write-Host ''
Write-Host 'Listo.' -ForegroundColor Green
Write-Host ("EXE portable: {0}" -f (Join-Path $appDir 'FileSender.exe'))
Write-Host ("ZIP: {0}" -f (Join-Path $repoDist 'FileSender-Windows-1.0.zip'))
Write-Host ("Instalador MSI: {0}" -f (Join-Path $repoDist 'FileSender-Windows-1.0-Setup.msi'))
Write-Host ("Tamaño ZIP: {0:N1} MB" -f ((Get-Item (Join-Path $repoDist 'FileSender-Windows-1.0.zip')).Length / 1MB))
Write-Host ("Tamaño MSI: {0:N1} MB" -f ((Get-Item (Join-Path $repoDist 'FileSender-Windows-1.0-Setup.msi')).Length / 1MB))
