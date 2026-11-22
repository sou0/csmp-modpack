param([int]$TimeoutSeg = 900, [switch]$SemSync)
# Teste de fumaca: sobe um servidor com mundo novo, confere se carrega, testa o datapack
# do Medidor de Caos por comandos, desliga e gera um relatorio.
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"

$mundo = 'mundo-fumaca'
$inicio = Get-Date
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'
$relDir = "$Teste\relatorios"
New-Item -ItemType Directory -Force $relDir | Out-Null
$relatorio = "$relDir\teste-servidor-$ts.txt"
$out = New-Object System.Collections.Generic.List[string]
function Say([string]$m) { Write-Host $m; $out.Add($m) }

# EULA precisa ter sido aceito por voce
$eula = "$Servidor\eula.txt"
if (-not (Test-Path $eula) -or -not (Select-String -Path $eula -Pattern '^eula=true' -Quiet)) {
    Say 'EULA do Minecraft ainda nao foi aceito. Leia https://aka.ms/MinecraftEULA e, se concordar, troque eula=false por eula=true em servidor\eula.txt'
    exit 2
}
if (Get-NetTCPConnection -LocalPort 25565 -State Listen -ErrorAction SilentlyContinue) {
    Say 'A porta 25565 ja esta em uso (outro servidor aberto?). Feche-o e tente de novo.'
    exit 2
}

# mundo novo a cada teste
$mundoDir = "$Servidor\$mundo"
if (Test-Path $mundoDir) { Remove-Item $mundoDir -Recurse -Force }
$logPath = "$Servidor\logs\latest.log"
if (Test-Path $logPath) { Remove-Item $logPath -Force -ErrorAction SilentlyContinue }

if (-not $SemSync) { Say '[1/4] Sincronizando com Pack-Direto...'; Sync-Servidor $mundo }

function Ler-Log {
    try {
        $fs = [IO.File]::Open($logPath, 'Open', 'Read', 'ReadWrite')
        $sr = New-Object IO.StreamReader($fs); $t = $sr.ReadToEnd(); $sr.Close(); return $t
    } catch { return '' }
}

Say '[2/4] Iniciando servidor (mundo novo)...'
$psi = New-Object Diagnostics.ProcessStartInfo
$psi.FileName = $Java
$psi.WorkingDirectory = $Servidor
$psi.Arguments = "@user_jvm_args.txt @$WinArgs --world $mundo nogui"
$psi.UseShellExecute = $false; $psi.CreateNoWindow = $true
$psi.RedirectStandardInput = $true; $psi.RedirectStandardOutput = $true; $psi.RedirectStandardError = $true
$p = [Diagnostics.Process]::Start($psi)
$outTask = $p.StandardOutput.ReadToEndAsync(); $errTask = $p.StandardError.ReadToEndAsync()
# escreve comandos em UTF-8 SEM BOM (o BOM estragava o primeiro comando)
$stdin = New-Object IO.StreamWriter($p.StandardInput.BaseStream, (New-Object Text.UTF8Encoding($false)))
$stdin.AutoFlush = $true

$sw = [Diagnostics.Stopwatch]::StartNew(); $pronto = $false
while ($sw.Elapsed.TotalSeconds -lt $TimeoutSeg -and -not $p.HasExited) {
    Start-Sleep -Seconds 3
    if ((Ler-Log) -match '\]: Done \(') { $pronto = $true; break }
}
$tempoCarga = [int]$sw.Elapsed.TotalSeconds

$testes = New-Object System.Collections.Generic.List[object]
function Cmd([string]$c) {
    $antes = (Ler-Log).Length
    $stdin.WriteLine($c)
    Start-Sleep -Milliseconds 2000
    $t = Ler-Log
    if ($t.Length -gt $antes) { return $t.Substring($antes) } else { return '' }
}
function Teste([string]$nome, [bool]$ok, [string]$detalhe = '') {
    $testes.Add([pscustomobject]@{ Nome = $nome; Ok = $ok; Detalhe = $detalhe })
}

if ($pronto) {
    Say "[3/4] Servidor carregou em ${tempoCarga}s. Testando datapack do Caos..."
    $null = Cmd ''   # linha de descarte: absorve o caractere invisivel (BOM) que o Windows coloca no 1o comando
    $null = Cmd 'function csmp_caos:admin/config_segundos {valor:9999}'   # congela a queda durante o teste
    $r = Cmd 'scoreboard players get #caos csmp_caos'
    Teste 'Datapack carregou (placar #caos existe)' ($r -match '#caos has -?\d+ \[csmp_caos\]') $r.Trim()
    $null = Cmd 'function csmp_caos:admin/definir {valor:0}'
    $null = Cmd 'function csmp_caos:admin/adicionar {valor:30}'
    $r = Cmd 'scoreboard players get #caos csmp_caos'
    Teste 'Adicionar +30 -> 30' ($r -match '#caos has 30 ') $r.Trim()
    $null = Cmd 'function csmp_caos:admin/definir {valor:150}'
    $r = Cmd 'scoreboard players get #caos csmp_caos'
    Teste 'Limite maximo (150 vira 100)' ($r -match '#caos has 100 ') $r.Trim()
    $null = Cmd 'function csmp_caos:admin/remover {valor:40}'
    $r = Cmd 'bossbar get csmp_caos:medidor value'
    Teste 'Barra acompanha o valor (100-40 = 60)' ($r -match 'value of 60') $r.Trim()
    $null = Cmd 'function csmp_caos:admin/remover {valor:500}'
    $r = Cmd 'scoreboard players get #caos csmp_caos'
    Teste 'Limite minimo (nao fica negativo)' ($r -match '#caos has 0 ') $r.Trim()
    # Clima por nivel: o datapack registra o nivel de clima aplicado em #nivel_clima.
# (O /weather em si so aparece de fato perto de jogadores com o Create: Skys, entao aqui
#  conferimos o nivel acionado; o visual da tempestade se confere dentro do jogo.)
function NivelClima([int]$caos, [int]$esperado, [string]$nome) {
    $null = Cmd "function csmp_caos:admin/definir {valor:$caos}"
    $r = Cmd 'scoreboard players get #nivel_clima csmp_cfg'
    Teste $nome ($r -match "#nivel_clima has $esperado ") $r.Trim()
}
NivelClima 30 0 'Clima abaixo de 50% -> normal (sem tempestade)'
NivelClima 60 2 'Clima 50-74% -> tempestade'
NivelClima 90 3 'Clima 75%+ -> apocalipse (tempestade + raios)'
NivelClima 0  0 'Clima 0% -> normal'
# Mob do mod csmp: o Monstro Corrupto existe e pode ser invocado
    $null = Cmd 'summon csmp:monstro_corrupto ~ ~ ~'
    $r = Cmd 'execute if entity @e[type=csmp:monstro_corrupto]'
    Teste 'Mob Monstro Corrupto pode ser invocado (mod csmp)' ($r -match 'Test passed') $r.Trim()
    $null = Cmd 'kill @e[type=csmp:monstro_corrupto]'
    # NPC Profeta: pode ser invocado e e invulneravel
    $null = Cmd 'summon csmp:profeta ~ ~ ~'
    $r = Cmd 'execute if entity @e[type=csmp:profeta]'
    Teste 'NPC Profeta pode ser invocado (mod csmp)' ($r -match 'Test passed') $r.Trim()
    $null = Cmd 'damage @e[type=csmp:profeta,limit=1] 5 minecraft:generic'
    $r = Cmd 'execute if entity @e[type=csmp:profeta,nbt={Health:20.0f}]'
    Teste 'NPC Profeta nao sofre dano (vida continua 20)' ($r -match 'Test passed') $r.Trim()
    $null = Cmd 'kill @e[type=csmp:profeta]'
    # Dimensao Vazio: gera o chao proprio e aceita a arvore preta gigante
    $null = Cmd 'execute in csmp:vazio run forceload add 0 0 95 95'
    Start-Sleep -Seconds 10
    $r = Cmd 'execute in csmp:vazio if block 0 3 0 csmp:chao_vazio'
    Teste 'Dimensao Vazio gera o chao (csmp:chao_vazio)' ($r -match 'Test passed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio if block 0 0 0 minecraft:bedrock'
    Teste 'Dimensao Vazio tem bedrock embaixo' ($r -match 'Test passed') $r.Trim()
    # limpa o espaco (arvores geradas sozinhas podem estar no caminho) e coloca uma arvore
    $null = Cmd 'execute in csmp:vazio run fill 0 4 0 20 60 20 minecraft:air'
    $r = Cmd 'execute in csmp:vazio run place feature csmp:arvore_negra 8 4 8'
    Teste 'Arvore negra gigante e colocada no Vazio' ($r -match 'Placed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio if block 8 6 8 csmp:tronco_negro'
    Teste 'Tronco negro existe onde a arvore foi colocada' ($r -match 'Test passed') $r.Trim()
    # Ponto de sculk: coloca sculk e seca o entorno (folhas somem, tronco vira negro, grama vira terra seca)
    $null = Cmd 'execute in csmp:vazio run fill 24 4 0 40 30 20 minecraft:air'
    $null = Cmd 'execute in csmp:vazio run place feature minecraft:oak 30 4 8'
    $r = Cmd 'execute in csmp:vazio if block 30 4 8 minecraft:oak_log'
    Teste 'Preparo: carvalho vivo colocado antes do ponto de sculk' ($r -match 'Test passed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio run place feature csmp:ponto_sculk 31 4 9'
    Teste 'Ponto de sculk (csmp:ponto_sculk) e colocado' ($r -match 'Placed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio if block 30 4 8 csmp:tronco_negro'
    Teste 'Ponto de sculk transforma o tronco vivo em tronco negro' ($r -match 'Test passed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio run fill 22 4 0 40 30 20 minecraft:glass replace minecraft:oak_leaves'
    Teste 'Ponto de sculk remove as folhas vivas em volta' ($r -match 'No blocks were filled|Could not set') $r.Trim()
    $r = Cmd 'execute in csmp:vazio run fill 22 4 0 40 40 20 minecraft:glass replace csmp:folhas_negras'
    Teste 'Ponto de sculk deixa uma copa rala de folhas negras' ($r -match 'Successfully filled') $r.Trim()
    # Sculk em pontos aleatorios: o sculk so se espalha em pedra/terra, entao usa um quadrado de pedra
    $null = Cmd 'execute in csmp:vazio run fill 40 3 40 56 3 56 minecraft:stone'
    $r = Cmd 'execute in csmp:vazio run place feature csmp:sculk_superficie 48 4 48'
    Teste 'Sculk de superficie (csmp:sculk_superficie) e colocado' ($r -match 'Placed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio run fill 36 3 36 60 6 60 minecraft:glass replace minecraft:sculk'
    Teste 'Sculk de superficie realmente espalha blocos de sculk' ($r -match 'Successfully filled') $r.Trim()
    $null = Cmd 'execute in csmp:vazio run fill 70 3 70 86 3 86 minecraft:stone'
    $r = Cmd 'execute in csmp:vazio run place feature csmp:sculk_caverna 78 4 78'
    Teste 'Sculk de caverna (csmp:sculk_caverna) e colocado' ($r -match 'Placed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio run fill 66 3 66 90 6 90 minecraft:glass replace minecraft:sculk'
    Teste 'Sculk de caverna realmente espalha blocos de sculk' ($r -match 'Successfully filled') $r.Trim()
# Persistencia: grava 42, desliga, religa o MESMO mundo e confere se o caos continua 42
    $null = Cmd 'function csmp_caos:admin/definir {valor:42}'
    $null = Cmd 'function csmp_caos:admin/config_ganho {valor:7}'
    Say '[4/5] Desligando servidor (valor do caos = 42)...'
    $stdin.WriteLine('stop')
    if (-not $p.WaitForExit(120000)) { Say '  nao desligou em 120s, forcando.'; $p.Kill() }
    $log1 = Ler-Log
    Copy-Item $logPath "$relDir\teste-servidor-$ts.log" -ErrorAction SilentlyContinue
    Say '[5/5] Religando o mesmo mundo para conferir se o caos ficou salvo...'
    Remove-Item $logPath -Force -ErrorAction SilentlyContinue
    $p = [Diagnostics.Process]::Start($psi)
    $outTask = $p.StandardOutput.ReadToEndAsync(); $errTask = $p.StandardError.ReadToEndAsync()
    $stdin = New-Object IO.StreamWriter($p.StandardInput.BaseStream, (New-Object Text.UTF8Encoding($false)))
    $stdin.AutoFlush = $true
    $sw2 = [Diagnostics.Stopwatch]::StartNew(); $pronto2 = $false
    while ($sw2.Elapsed.TotalSeconds -lt $TimeoutSeg -and -not $p.HasExited) {
        Start-Sleep -Seconds 3
        if ((Ler-Log) -match '\]: Done \(') { $pronto2 = $true; break }
    }
    if ($pronto2) {
        $null = Cmd ''
        $r = Cmd 'scoreboard players get #caos csmp_caos'
        Teste 'Caos salvo apos reiniciar o servidor (42)' ($r -match '#caos has 42 ') $r.Trim()
        $r = Cmd 'bossbar get csmp_caos:medidor value'
        Teste 'Barra volta com o valor salvo (42)' ($r -match 'value of 42') $r.Trim()
        $r = Cmd 'scoreboard players get #ganho csmp_cfg'
        Teste 'Configuracao tambem fica salva (ganho 7)' ($r -match '#ganho has 7 ') $r.Trim()
    } else { Teste 'Servidor religou para o teste de persistencia' $false 'nao ficou pronto' }
    $stdin.WriteLine('stop')
    if (-not $p.WaitForExit(120000)) { $p.Kill() }
} else {
    Say "[3/4] Servidor NAO ficou pronto (saiu ou excedeu ${TimeoutSeg}s)."
    if (-not $p.HasExited) { $p.Kill() }
    Start-Sleep -Seconds 2
}

# ---- analise do log ----
$log = Ler-Log
$linhas = $log -split "`r?`n"
$erros = @($linhas | Where-Object { $_ -match '/ERROR\]' })
$avisos = @($linhas | Where-Object { $_ -match '/WARN\]' })
$crash = Get-ChildItem "$Servidor\crash-reports" -Filter *.txt -ErrorAction SilentlyContinue | Where-Object { $_.LastWriteTime -gt $inicio } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $log1) { Copy-Item $logPath "$relDir\teste-servidor-$ts.log" -ErrorAction SilentlyContinue }

Say ''
Say '================ RESULTADO ================'
foreach ($t in $testes) { Say ('  [{0}] {1}' -f $(if ($t.Ok) { 'OK ' } else { 'FALHOU' }), $t.Nome); if (-not $t.Ok) { Say "         resposta: $($t.Detalhe)" } }
Say "  Erros no log: $($erros.Count) | Avisos: $($avisos.Count)"
if ($crash) { Say "  Crash report: $($crash.FullName)" }
if ($erros.Count -gt 0) {
    Say ''; Say '--- Erros agrupados por origem (quantidade + 1o exemplo) ---'
    $grupos = $erros | Group-Object { if ($_ -match '/ERROR\] \[([^\]]+)\]') { $matches[1] } else { '?' } } | Sort-Object Count -Descending
    foreach ($g in $grupos) {
        Say ('  {0,4}x {1}' -f $g.Count, $g.Name)
        Say ('        ex.: ' + ($g.Group[0] -replace '^\[[^\]]+\] ', ''))
    }
}
if (-not $pronto) {
    Say ''; Say '--- Final do log ---'; $linhas | Select-Object -Last 60 | ForEach-Object { Say "  $_" }
    if ($errTask.IsCompleted -and $errTask.Result.Trim()) {
        Say ''; Say '--- Saida de erro do Java (stderr) ---'
        ($errTask.Result -split "`r?`n") | Select-Object -First 40 | ForEach-Object { Say "  $_" }
    }
}

$falhou = (-not $pronto) -or (@($testes | Where-Object { -not $_.Ok }).Count -gt 0)
Say ''
if ($falhou) { Say 'VEREDITO: FALHOU' }
elseif ($erros.Count -gt 0) { Say 'VEREDITO: PASSOU, MAS COM ERROS NO LOG (veja acima)' }
else { Say 'VEREDITO: PASSOU' }
Say "Relatorio salvo em: $relatorio"
$out | Set-Content -Path $relatorio -Encoding UTF8
if ($falhou) { exit 1 } else { exit 0 }
