$ErrorActionPreference = 'Stop'

$principal = New-Object Security.Principal.WindowsPrincipal(
    [Security.Principal.WindowsIdentity]::GetCurrent())
$isAdmin = $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Start-Process powershell.exe -Verb RunAs -ArgumentList @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', "`"$PSCommandPath`""
    )
    exit
}

$exe = Join-Path $PSScriptRoot 'FileSender.exe'
if (-not (Test-Path -LiteralPath $exe)) {
    Write-Host 'No se encuentra FileSender.exe junto a este script.' -ForegroundColor Red
    Read-Host 'Pulsa Enter para cerrar'
    exit 1
}

$rules = @(
    @{ Name = 'FileSender - TCP'; Protocol = 'TCP'; Port = 47822 },
    @{ Name = 'FileSender - UDP'; Protocol = 'UDP'; Port = 47821 }
)

foreach ($rule in $rules) {
    Get-NetFirewallRule -DisplayName $rule.Name -ErrorAction SilentlyContinue |
        Remove-NetFirewallRule -ErrorAction SilentlyContinue
    New-NetFirewallRule `
        -DisplayName $rule.Name `
        -Direction Inbound `
        -Action Allow `
        -Program $exe `
        -Protocol $rule.Protocol `
        -LocalPort $rule.Port `
        -Profile Private,Domain `
        -Description 'FileSender: transferencia local de archivos por WiFi' | Out-Null
}

Write-Host ''
Write-Host 'FileSender ya puede recibir conexiones en redes privadas.' -ForegroundColor Green
Write-Host 'Puertos permitidos: TCP 47822 y UDP 47821.'
Write-Host ''
Read-Host 'Pulsa Enter para cerrar'
