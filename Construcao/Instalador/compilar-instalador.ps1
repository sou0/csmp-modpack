# Compila o CSMP Atualizador (csc do .NET Framework 4, que ja vem no Windows) e monta o instalador com o Inno Setup.
# Saida para os jogadores: <raiz do repo>\Instalador\CSMP-Setup.exe
# Tambem atualiza <raiz>\Instalador\CSMP.exe (copia do atualizador), que as versoes antigas (1 a 5) baixavam sozinhas.
# Se mudar o programa, suba o numero em versao.txt E em Programa.VERSAO no CSMP.cs.
$ErrorActionPreference = 'Stop'
$aqui = $PSScriptRoot
$raiz = Split-Path (Split-Path $aqui -Parent) -Parent
$saida = Join-Path $aqui 'saida'
New-Item -ItemType Directory -Force $saida, (Join-Path $raiz 'Instalador') | Out-Null
$ver = (Get-Content "$aqui\versao.txt" | Select-Object -First 1).Trim()
$codigo = Get-Content "$aqui\CSMP.cs" -Raw
if ($codigo -notmatch "VERSAO = $ver;") { throw "versao.txt ($ver) e Programa.VERSAO no CSMP.cs estao diferentes" }

$csc = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
& $csc /nologo /codepage:65001 /target:winexe /optimize+ /win32icon:"$aqui\csmp.ico" /out:"$saida\CSMPAtualizador.exe" `
    /r:System.Web.Extensions.dll /r:System.Windows.Forms.dll /r:System.Drawing.dll "$aqui\CSMP.cs"
if ($LASTEXITCODE -ne 0) { throw "csc falhou" }

& python -I "$aqui\imagens-instalador.py" $saida | Out-Null
if ($LASTEXITCODE -ne 0) { throw "falhou ao gerar as imagens do instalador" }

$iscc = Join-Path (Split-Path $aqui -Parent) 'Ambiente-de-Teste\_ferramentas\innosetup\ISCC.exe'
& $iscc /Q "/DVersao=$ver" "$aqui\CSMP.iss"
if ($LASTEXITCODE -ne 0) { throw "Inno Setup falhou" }
Copy-Item "$saida\CSMPAtualizador.exe" (Join-Path $raiz 'Instalador\CSMP.exe') -Force
Write-Host "Instalador pronto: $raiz\Instalador\CSMP-Setup.exe (versao $ver)"
