# Instalables de FileSender 1.0

Todos los archivos están en la carpeta [`dist/`](../dist/) del repositorio y también
se pueden adjuntar como *assets* de una Release de GitHub.

| Plataforma | Archivo | Tamaño | Instalación |
|---|---|---:|---|
| Android 7+ | [`FileSender-1.0.apk`](../dist/FileSender-1.0.apk) | 96 KB | Abrir el APK en la tablet y permitir la instalación desde orígenes desconocidos. |
| Windows 10/11 x64 | [`FileSender-Windows-1.0-Setup.msi`](../dist/FileSender-Windows-1.0-Setup.msi) | 48,7 MB | Doble clic en el MSI y seguir el asistente. |
| Windows 10/11 x64 | [`FileSender-Windows-1.0.zip`](../dist/FileSender-Windows-1.0.zip) | 47,8 MB | Descomprimir y ejecutar `FileSender.exe`. No requiere instalación. |

## Requisitos

- Android 7.0 o superior.
- Windows 10 u 11 de 64 bits.
- Ambos dispositivos en la misma red WiFi.
- Firewall de Windows permitiendo FileSender en redes privadas.

## Windows: instalador MSI

1. Descarga `FileSender-Windows-1.0-Setup.msi`.
2. Ejecútalo y sigue el asistente.
3. Se crean accesos directos en el escritorio y en el menú Inicio.
4. Se instala por usuario; no es necesario instalar Java.

Desinstalación: **Configuración → Aplicaciones → FileSender → Desinstalar**.

Si el móvil no encuentra el PC, ejecuta `PermitirFirewall.bat` desde la carpeta
de instalación y acepta el aviso de administrador.

## Windows: versión portátil

1. Descomprime el ZIP completo.
2. Ejecuta `FileSender.exe`.
3. Para retirar el Firewall, usa `QuitarFirewall.bat`.

## Android

1. Copia `FileSender-1.0.apk` a la tablet o descárgalo desde la Release.
2. Ábrelo y permite la instalación desde orígenes desconocidos.
3. Concede el permiso de almacenamiento la primera vez que pulses **Recibir**.

Los archivos recibidos se guardan en:

- Android: `Descargas/FileSender/`
- Windows: `%USERPROFILE%\Downloads\FileSender`

## Sumas SHA-256

```text
7e07514ad003a20c170912dba56752e4ddfb94158f13a0abf20420d3fba829f9  FileSender-1.0.apk
85a3a3fad745688a0bdb7e2b0da38ac078efcdf0293efef2af84579f55cac16a  FileSender-Windows-1.0-Setup.msi
3b17db1d4909e240e625133a4458cebba5d8883255d5e72515c220859729821f  FileSender-Windows-1.0.zip
```

Los mismos valores están en los archivos `.sha256` de `dist/`.

## Publicar los instalables en GitHub Releases

Los binarios ya están versionados en `dist/`, así que también se pueden descargar
directamente del repositorio. La etiqueta `v1.0.0` ya está creada en el
repositorio local. Para que los archivos aparezcan como *assets* de una Release:

1. Sube el repositorio y la etiqueta a GitHub (con el repo vacío ya creado):

   ```powershell
   git remote add origin https://github.com/UTILIZADOR/REPOSITORIO.git
   git push -u origin main
   git push origin v1.0.0
   ```

2. En GitHub: **Releases → Draft a new release**.
3. Selecciona la etiqueta `v1.0.0`, pon el título `FileSender 1.0` y publica.
4. Arrastra a la Release estos archivos desde `dist/`:
   - `FileSender-1.0.apk`
   - `FileSender-Windows-1.0-Setup.msi`
   - `FileSender-Windows-1.0.zip`
   - los tres archivos `.sha256`

También se puede hacer con GitHub CLI, si está instalado:

```powershell
gh release create v1.0.0 `
  dist/FileSender-1.0.apk `
  dist/FileSender-Windows-1.0-Setup.msi `
  dist/FileSender-Windows-1.0.zip `
  --title "FileSender 1.0" `
  --notes "Primera versión para Android 7+ y Windows 10/11."
```

## Notas de seguridad

- La transferencia es local, por TCP, y no está cifrada.
- No se usa Bluetooth, cuenta ni servidor en la nube.
- No se requiere permiso de administrador para usar la app; solo para crear las
  reglas del Firewall con el script incluido.
- Si Windows SmartScreen muestra un aviso por no tener firma comercial, elige
  **Más información → Ejecutar de todas formas**. Como alternativa, usa la
  versión portátil.
