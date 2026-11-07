@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0PermitirFirewall.ps1"
if errorlevel 1 pause
