# Cria um MAPA NOVO para o servidor de teste:
#  - APAGA o mundo atual (servidor\world) e qualquer 'world-antigo-*' que tenha sobrado (decisao do dono do projeto:
#    mundos de teste antigos nao sao guardados);
#  - apaga as folhas caidas que o Rustling Leaves salvou no cliente (ver Limpar-FolhasSalvas em comum.ps1).
# So roda com o servidor e o jogo FECHADOS.
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"

if (Get-NetTCPConnection -LocalPort 25565 -State Listen -ErrorAction SilentlyContinue) {
    Write-Host 'Feche o servidor antes (digite stop na janela dele).'; exit 1
}
if (Get-CimInstance Win32_Process -Filter "Name='javaw.exe'" | Where-Object { $_.CommandLine -match 'net.minecraft.client.main.Main' }) {
    Write-Host 'Feche o jogo antes (ele salva as folhas ao sair e desfaria a limpeza).'; exit 1
}

$apagar = @("$Servidor\world") + @(Get-ChildItem $Servidor -Directory -Filter 'world-antigo-*' | ForEach-Object { $_.FullName })
foreach ($pasta in $apagar) {
    # trava de seguranca: so mexe em pastas de mundo DENTRO da pasta do servidor de teste
    if ((Test-Path $pasta) -and $pasta.StartsWith($Servidor) -and ((Split-Path $pasta -Leaf) -like 'world*')) {
        Remove-Item $pasta -Recurse -Force
        Write-Host "Mundo apagado: $(Split-Path $pasta -Leaf)"
    }
}
Limpar-FolhasSalvas
Write-Host 'Pronto. Abra o servidor (3-SERVIDOR-ABERTO.bat); o mundo novo e criado na primeira vez.'
