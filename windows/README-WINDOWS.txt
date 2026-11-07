FileSender para Windows
=======================

Aplicación de escritorio para enviar y recibir archivos entre este PC y la app
FileSender de Android por la red WiFi local. No usa Bluetooth, cuentas ni
servicios en la nube.

Requisitos
----------
- Windows 10 u 11 de 64 bits.
- Las dos máquinas en la misma red WiFi.
- El otro dispositivo debe tener FileSender abierto en la pantalla Recibir.


Instalar / abrir
----------------
Hay dos opciones:

A) Instalador recomendado:
   1. Ejecuta FileSender-Windows-1.0-Setup.msi.
   2. Elige la carpeta de instalación y finaliza el asistente.
   3. Se crean accesos directos en el escritorio y en el menú Inicio.
   4. Se instala por usuario, sin permisos de administrador.

B) Versión portátil:
   1. Descomprime la carpeta completa del ZIP.
   2. Ejecuta FileSender.exe. No hace falta instalar Java.

En ambos casos, si Windows SmartScreen muestra un aviso, elige "Más información"
y "Ejecutar de todas formas" (el ejecutable no tiene firma comercial).
La primera vez, Windows puede preguntar por el Firewall. Permite el acceso en
redes privadas.

Si no aparece el aviso o el móvil no encuentra el PC:
- Ejecuta PermitirFirewall.bat y acepta el aviso de administrador.
- Para retirar esas reglas, ejecuta QuitarFirewall.bat.


Recibir en el PC
----------------
1. Pulsa Recibir.
2. Deja la ventana abierta. Se mostrará la IP y el puerto del PC.
3. En el móvil, abre FileSender, pulsa Enviar, elige los archivos y toca el PC.
4. Los archivos se guardan en:

   %USERPROFILE%\Downloads\FileSender

El botón "Abrir carpeta de Descargas" abre esa carpeta.


Enviar desde el PC
------------------
1. Pulsa Enviar.
2. Arrastra uno o varios archivos a "Arrastra para compartir".
   También puedes hacer clic en esa zona para abrir el explorador de Windows
   y seleccionar varios archivos.
3. La app busca automáticamente dispositivos en la red.
4. Toca el móvil o tablet que esté en la pantalla Recibir.
5. La transferencia comienza y muestra el progreso.

Puedes añadir o quitar archivos antes de elegir el dispositivo. La búsqueda se
puede repetir con "Buscar de nuevo".


Notas
-----
- Puerto UDP de descubrimiento: 47821.
- Puerto TCP de transferencia: 47822.
- La transferencia es local y no está cifrada; usa una red de confianza.
- Algunos routers tienen "aislamiento de clientes" o "AP isolation". Si está
  activado, los dispositivos no se ven entre sí; hay que desactivarlo.
- Máximo 500 archivos por transferencia.
- Los archivos parciales se eliminan si la transferencia falla.
- La app no se conecta a Internet.


Compilar desde el código
------------------------
Requiere JDK 17 (con jpackage) en el PATH o JAVA_HOME configurado.

    cd windows
    .\build.ps1

El ejecutable portátil se genera en windows\dist\FileSender\FileSender.exe,
el ZIP en dist\FileSender-Windows-1.0.zip y el instalador MSI en
dist\FileSender-Windows-1.0-Setup.msi.

Para omitir pruebas o empaquetado:

    .\build.ps1 -SkipTests
    .\build.ps1 -SkipPackage


Terceros
--------
La interfaz usa FlatLaf 3.6.1 (Apache License 2.0).
Consulta THIRD-PARTY-LICENSES.txt.
