$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"
Write-Host 'Sincronizando instancia do cliente com CSMP-Direto-0.1...'
Sync-Cliente
Write-Host 'Abrindo Prism Launcher e iniciando a instancia CSMP-Teste...'
Start-Process -FilePath "$Prism\prismlauncher.exe" -ArgumentList '-l', 'CSMP-Teste' -WorkingDirectory $Prism
