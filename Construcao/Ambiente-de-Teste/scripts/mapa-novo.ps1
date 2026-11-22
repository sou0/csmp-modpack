# Cria um MAPA NOVO para o servidor de teste:
#  - guarda o mundo antigo (servidor\world -> servidor\world-antigo-<data>), sem apagar nada;
#  - apaga as folhas caidas que o Rustling Leaves salvou no cliente (ver Limpar-FolhasSalvas em comum.ps1).
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"

if (Get-NetTCPConnection -LocalPort 25565 -State Listen -ErrorAction SilentlyContinue) {
    Write-Host 'Feche o servidor antes (digite stop na janela dele).'; exit 1
}
if (Get-CimInstance Win32_Process -Filter "Name='javaw.exe'" | Where-Object { $_.CommandLine -match 'net.minecraft.client.main.Main' }) {
    Write-Host 'Feche o jogo antes (ele salva as folhas ao sair e desfaria a limpeza).'; exit 1
}

$ts = Get-Date -Format 'yyyyMMdd-HHmm'
if (Test-Path "$Servidor\world") {
    Rename-Item "$Servidor\world" "world-antigo-$ts"
    Write-Host "Mundo antigo guardado: servidor\world-antigo-$ts"
} else {
    Write-Host 'Nao havia mundo antigo.'
}
Limpar-FolhasSalvas
Write-Host 'Pronto. Abra o servidor (3-SERVIDOR-ABERTO.bat); o mundo novo e criado na primeira vez.'
