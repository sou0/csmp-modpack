$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"
Write-Host 'Sincronizando com CSMP-Direto-0.1...'
Sync-Servidor
Write-Host ''
Write-Host 'Iniciando servidor (mundo persistente "world"). Digite "stop" para fechar.'
Write-Host 'Dicas: /reload recarrega datapack | op SeuNome | conecte em localhost'
Set-Location $Servidor
& $Java '@user_jvm_args.txt' "@$WinArgs" nogui
