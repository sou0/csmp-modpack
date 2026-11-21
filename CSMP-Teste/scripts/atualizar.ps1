param([ValidateSet('ambos', 'servidor', 'cliente', 'tlauncher')][string]$Alvo = 'ambos')
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"
if ($Alvo -in 'ambos', 'servidor')  { Write-Host '[servidor]  sincronizando com CSMP-Direto-0.1'; Sync-Servidor }
if ($Alvo -in 'ambos', 'tlauncher') { Write-Host '[tlauncher] sincronizando com CSMP-Direto-0.1'; Sync-TLauncher }
if ($Alvo -in 'ambos', 'cliente')   { Write-Host '[prism]     sincronizando com CSMP-Direto-0.1'; Sync-Cliente }
Write-Host 'Pronto.'
