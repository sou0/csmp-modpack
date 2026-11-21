# Funcoes e caminhos compartilhados pelos scripts de teste do CSMP.
# Layout esperado:
#   <raiz>\CSMP-Direto-0.1   (pack, fonte da verdade)
#   <raiz>\CSMP-Teste        (este ambiente)

$Teste     = Split-Path $PSScriptRoot -Parent
$Raiz      = Split-Path $Teste -Parent
$Pack      = Join-Path $Raiz 'CSMP-Direto-0.1'
$JavaHome  = (Get-ChildItem "$Teste\_ferramentas\java21" -Directory | Select-Object -First 1).FullName
$Java      = "$JavaHome\bin\java.exe"
$Servidor  = "$Teste\servidor"
$Prism     = "$Teste\cliente\prism"
$Instancia = "$Prism\instances\CSMP-Teste\minecraft"
$WinArgs   = 'libraries/net/neoforged/neoforge/21.1.267/win_args.txt'
$TLauncher = "$Teste\cliente\tlauncher"   # pasta de jogo para usar no TLauncher (ou outro launcher)

function Espelhar([string]$de, [string]$para, [string[]]$excluir = @()) {
    if (-not (Test-Path $de)) { Write-Host "  (nao existe, pulando: $de)"; return }
    New-Item -ItemType Directory -Force $para | Out-Null
    $a = @($de, $para, '/MIR', '/NFL', '/NDL', '/NJH', '/NJS', '/NP', '/R:1', '/W:1')
    if ($excluir.Count) { $a += '/XF'; $a += $excluir }
    robocopy @a | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy falhou (codigo $LASTEXITCODE): $de" }
}

function Ler-Excluidos {
    $f = "$PSScriptRoot\servidor-excluir.txt"
    if (-not (Test-Path $f)) { return @() }
    Get-Content $f | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith('#') }
}

function Sync-Servidor([string]$mundo = 'world') {
    Write-Host '  mods...';        Espelhar "$Pack\mods"   "$Servidor\mods" (Ler-Excluidos)
    # /XF tambem impede o robocopy de apagar no destino: remove na mao o que ficou de syncs antigos
    foreach ($padrao in Ler-Excluidos) {
        Get-ChildItem "$Servidor\mods" -Filter $padrao -File -ErrorAction SilentlyContinue | Remove-Item -Force
    }
    Write-Host '  config...';      Espelhar "$Pack\config" "$Servidor\config"
    # O datapack do Caos agora vem DENTRO do mod csmp. Remove copias antigas soltas no mundo
    # (senao o caos seria contado em dobro).
    Write-Host '  datapacks (agora dentro do mod csmp)...'
    Remove-Item "$Servidor\$mundo\datapacks\csmp_caos" -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item "$Servidor\world\datapacks\csmp_caos" -Recurse -Force -ErrorAction SilentlyContinue
}

function Apontar-Menu-Local($pastaJogo) {
    # No teste, o botao "Entrar no CSMP" do FancyMenu aponta para o servidor local.
    # O pack em si mantem o IP fixo real (csmp.enxada.host).
    $dir = "$pastaJogo\config\fancymenu\customization"
    if (-not (Test-Path $dir)) { return }
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    Get-ChildItem $dir -Filter *.txt | ForEach-Object {
        $x = [IO.File]::ReadAllText($_.FullName)
        if ($x.Contains('csmp.enxada.host')) {
            [IO.File]::WriteAllText($_.FullName, $x.Replace('csmp.enxada.host','localhost'), $utf8)
            Write-Host "  menu -> localhost ($($_.Name))"
        }
    }
}

function Sync-TLauncher {
    # O TLauncher usa a pasta da propria versao ("CSMP") como diretorio de jogo dos mods
    $jogo = "$TLauncher\versions\CSMP"
    Write-Host '  mods...';          Espelhar "$Pack\mods"          "$jogo\mods"
    Write-Host '  config...';        Espelhar "$Pack\config"        "$jogo\config"
    Write-Host '  resourcepacks...'; Espelhar "$Pack\resourcepacks" "$jogo\resourcepacks"
    Apontar-Menu-Local $jogo
    $opt = "$jogo\options.txt"
    if (-not (Test-Path $opt)) {
        "lang:pt_br`nonboardAccessibility:false`nresourcePacks:[`"vanilla`"]`nincompatibleResourcePacks:[]`n" |
            Set-Content -Path $opt -Encoding ASCII
    }
}

function Sync-Cliente {
    Write-Host '  mods...';          Espelhar "$Pack\mods"          "$Instancia\mods"
    Write-Host '  config...';        Espelhar "$Pack\config"        "$Instancia\config"
    Write-Host '  resourcepacks...'; Espelhar "$Pack\resourcepacks" "$Instancia\resourcepacks"
    Apontar-Menu-Local $Instancia
    # options.txt so e criado se ainda nao existir (nao sobrescreve suas opcoes)
    $opt = "$Instancia\options.txt"
    if (-not (Test-Path $opt)) {
        "lang:pt_br`nonboardAccessibility:false`nresourcePacks:[`"vanilla`"]`nincompatibleResourcePacks:[]`n" |
            Set-Content -Path $opt -Encoding ASCII
    }
}

