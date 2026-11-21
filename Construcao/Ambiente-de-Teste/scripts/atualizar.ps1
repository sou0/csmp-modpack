param([ValidateSet('ambos', 'servidor', 'cliente', 'tlauncher')][string]$Alvo = 'ambos')
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"
if ($Alvo -in 'ambos', 'servidor')  { Write-Host '[servidor]  sincronizando com Pack-Direto'; Sync-Servidor }
if ($Alvo -in 'ambos', 'tlauncher') { Write-Host '[tlauncher] sincronizando com Pack-Direto'; Sync-TLauncher }
if ($Alvo -in 'ambos', 'cliente')   { Write-Host '[prism]     sincronizando com Pack-Direto'; Sync-Cliente }
if ($Alvo -eq 'ambos') { & (Join-Path $Raiz 'gerar-pasta-pronta.ps1') }
Write-Host 'Pronto.'
