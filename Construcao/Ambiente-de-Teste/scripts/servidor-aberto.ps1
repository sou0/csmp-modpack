$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"
Write-Host 'Sincronizando com Pack-Direto...'
Sync-Servidor
# Mundo novo a caminho (nao existe servidor\world)? Entao limpa as folhas salvas do cliente.
if (-not (Test-Path "$Servidor\world")) {
    Write-Host 'Mundo novo: limpando as folhas salvas do Rustling Leaves no cliente...'
    Limpar-FolhasSalvas
}
Write-Host ''
Write-Host 'Iniciando servidor (mundo persistente "world"). Digite "stop" para fechar.'
Write-Host 'Dicas: /reload recarrega datapack | op SeuNome | conecte em localhost'
Set-Location $Servidor
& $Java '@user_jvm_args.txt' "@$WinArgs" nogui
