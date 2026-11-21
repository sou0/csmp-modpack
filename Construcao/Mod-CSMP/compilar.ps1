# Compila o mod e copia o jar para o pack Direto. Feche o jogo/servidor antes (usa ~2-3 GB de RAM).
$ErrorActionPreference = 'Stop'
$aqui = $PSScriptRoot
$ferr = Join-Path (Split-Path $aqui) 'Ambiente-de-Teste\_ferramentas'
$env:JAVA_HOME = Get-ChildItem "$ferr\java21" -Directory | Select-Object -First 1 -ExpandProperty FullName
$env:GRADLE_USER_HOME = "$ferr\gradle-home"
$gradle = Get-ChildItem "$ferr\gradle" -Directory | Select-Object -First 1 -ExpandProperty FullName
Set-Location $aqui
$ErrorActionPreference = 'Continue'   # avisos do compilador vao para stderr e nao devem abortar
& "$gradle\bin\gradle.bat" build --console=plain
if ($LASTEXITCODE -ne 0) { Write-Host 'FALHOU a compilacao.'; exit 1 }
$pack = Join-Path (Split-Path $aqui) 'Pack-Direto\mods'
Get-ChildItem $pack -Filter 'csmp-core-*.jar' | Remove-Item -Force
Copy-Item "$aqui\build\libs\csmp-core-*.jar" $pack
Write-Host "Copiado para $pack"
& (Join-Path (Split-Path $aqui) 'gerar-pasta-pronta.ps1')
