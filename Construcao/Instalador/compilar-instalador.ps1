# Compila o CSMP.exe (instalador/atualizador) com o csc do .NET Framework 4 que ja vem no Windows.
# Saida: <raiz do repo>\Instalador\CSMP.exe (e dali que o programa se atualiza pelo GitHub).
# Se mudar o programa, suba o numero em versao.txt E em Programa.VERSAO no CSMP.cs, para os amigos receberem.
$ErrorActionPreference = 'Stop'
$aqui = $PSScriptRoot
$raiz = Split-Path (Split-Path $aqui -Parent) -Parent
$saida = Join-Path $raiz 'Instalador'
New-Item -ItemType Directory -Force $saida | Out-Null
$csc = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
$ver = (Get-Content "$aqui\versao.txt" | Select-Object -First 1).Trim()
$codigo = Get-Content "$aqui\CSMP.cs" -Raw
if ($codigo -notmatch "VERSAO = $ver;") { throw "versao.txt ($ver) e Programa.VERSAO no CSMP.cs estao diferentes" }
& $csc /nologo /codepage:65001 /target:winexe /optimize+ /win32icon:"$aqui\csmp.ico" /out:"$saida\CSMP.exe" `
    /r:System.Web.Extensions.dll /r:System.Windows.Forms.dll /r:System.Drawing.dll "$aqui\CSMP.cs"
if ($LASTEXITCODE -ne 0) { throw "csc falhou" }
Write-Host "Instalador compilado: $saida\CSMP.exe (versao $ver)"
