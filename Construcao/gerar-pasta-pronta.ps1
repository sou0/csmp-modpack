# Gera a pasta "Pronto-para-Minecraft" (ao lado de Construcao) a partir de Construcao\Pack-Direto.
# Ela contem so o que vai para a pasta do jogo: mods, config, resourcepacks + COMO-INSTALAR.txt.
$ErrorActionPreference = 'Stop'
$Raiz   = $PSScriptRoot
$Pack   = Join-Path $Raiz 'Pack-Direto'
$Pronto = Join-Path (Split-Path $Raiz -Parent) 'Pronto-para-Minecraft'

New-Item -ItemType Directory -Force $Pronto | Out-Null
foreach ($pasta in 'mods', 'config', 'resourcepacks') {
    if (-not (Test-Path "$Pack\$pasta")) { New-Item -ItemType Directory -Force "$Pack\$pasta" | Out-Null }
    robocopy "$Pack\$pasta" "$Pronto\$pasta" /MIR /NFL /NDL /NJH /NJS /NP /R:1 /W:1 | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy falhou (codigo $LASTEXITCODE): $pasta" }
}
$qtd = @(Get-ChildItem "$Pronto\mods" -Filter *.jar -File).Count
$guia = [IO.File]::ReadAllText("$Pack\LEIA-ME.txt").Replace('{MODS}', "$qtd")
[IO.File]::WriteAllText("$Pronto\COMO-INSTALAR.txt", $guia, (New-Object Text.UTF8Encoding($false)))
Write-Host "Pasta pronta atualizada: $Pronto  ($qtd mods)"
$global:LASTEXITCODE = 0
