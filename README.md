# FileSender

Aplicación Android y de escritorio Windows para enviar y recibir archivos entre dispositivos de la misma red WiFi. No usa Bluetooth, cuentas ni Internet.

Pensada para una tablet Android 7 usada como tablet de cocina y para pasar archivos entre el PC y el móvil.

## Instalables

- [Guía de instalables y assets de GitHub](docs/INSTALABLES.md)
- APK Android: `dist/FileSender-1.0.apk`
- Instalador Windows: `dist/FileSender-Windows-1.0-Setup.msi`
- Windows portátil: `dist/FileSender-Windows-1.0.zip`

## Windows

- `dist/FileSender-Windows-1.0-Setup.msi`: instalador recomendado. Crea accesos directos y se instala por usuario.
- `dist/FileSender-Windows-1.0.zip`: versión portátil; descomprimir y ejecutar `FileSender.exe`.
- No hace falta instalar Java.
- Pantalla principal con **Recibir** y **Enviar**.
- Para enviar: arrastra uno o varios archivos a la zona **Arrastra para compartir** o haz clic en ella para abrir el explorador de Windows.
- Los archivos recibidos se guardan en `%USERPROFILE%\Downloads\FileSender`.
- Si el móvil no encuentra el PC, ejecuta `PermitirFirewall.bat` desde la carpeta de instalación o desde el ZIP (pide permisos de administrador). También se incluye `QuitarFirewall.bat`.
- Instrucciones completas en `README-WINDOWS.txt`, dentro del ZIP y de la carpeta de instalación.

## Android

- `dist/FileSender-1.0.apk`
- `minSdkVersion 24` (Android 7.0)
- Sin dependencias de terceros; APK de ~100 KB
- Firma de instalación local incluida (el APK se puede instalar directamente)

## Cómo usarla

### Recibir

1. Abre FileSender en la tablet de cocina.
2. Pulsa **Recibir** y concede el permiso de almacenamiento la primera vez.
3. Deja esa pantalla abierta. La tablet queda visible en la red.
4. Los archivos se guardan en:

   `Descargas/FileSender/`

   (ruta real: `/storage/emulated/0/Download/FileSender/`)

### Enviar desde la app

1. Pulsa **Enviar**.
2. Selecciona uno o varios archivos.
3. La app busca automáticamente tablets que estén en la pantalla **Recibir**.
4. Toca el dispositivo y la transferencia empieza.

### Enviar desde el menú Compartir

1. En el gestor de archivos, selecciona uno o varios archivos.
2. Pulsa **Compartir** y elige **FileSender**.
3. La app abre la pantalla de envío y busca dispositivos.
4. Toca la tablet que está en **Recibir**.

## Requisitos y notas

- Los dos dispositivos deben estar en la misma red WiFi. No funciona entre redes distintas ni con aislamiento de clientes del router.
- El dispositivo receptor debe tener FileSender abierto en la pantalla **Recibir**.
- La transferencia es por TCP local, sin cifrar. Está pensada para una red doméstica de confianza.
- No se usa Bluetooth.
- Puertos usados: UDP `47821` para descubrimiento y TCP `47822` para datos.
- Si el router bloquea la difusión UDP, los dispositivos no aparecerán. En ese caso hay que desactivar el aislamiento de clientes/AP isolation en el router.
- En Android 10 o superior los archivos se guardan con MediaStore en la misma carpeta visible `Download/FileSender`.
- Límite de 500 archivos por transferencia.
- Si una transferencia se interrumpe, el archivo parcial se elimina.

## Compilar Android

Requisitos: JDK 17 y Android SDK con la plataforma 35.

```powershell
.\gradlew.bat assembleRelease
```

El APK se genera en:

`app/build/outputs/apk/release/app-release.apk`

La variante release se firma con la clave de depuración local para poder instalarla
directamente en la tablet. Para distribuirla de forma permanente, cambia la
`signingConfig` en `app/build.gradle` por una clave propia.

## Compilar Windows

Requiere JDK 17 con `jpackage` y PowerShell.

```powershell
cd windows
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Genera `windows\dist\FileSender\FileSender.exe`, el ZIP
`dist\FileSender-Windows-1.0.zip` y el instalador
`dist\FileSender-Windows-1.0-Setup.msi`. El script ejecuta las pruebas
automáticas antes de empaquetar y descarga WiX Toolset la primera vez si hace falta.

## Pruebas

Android:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Windows:

```powershell
cd windows
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Las pruebas incluyen recorridos completos cliente-servidor por TCP con varios
archivos, archivo vacío, tamaño desconocido, nombres peligrosos y verificación
byte a byte, además de descubrimiento UDP y descubrimiento real en la red local.
