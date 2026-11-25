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
    Start-Sleep -Seconds 20   # com muitos mods, o servidor ainda termina de recarregar (Patchouli etc.) logo apos o Done
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
    # o chao pode estar coberto por rua ou casa num ponto qualquer; procura numa area de 33x33
    $chao = $false
    foreach ($px in -12, 0, 12) { foreach ($pz in -12, 0, 12) {
        if (-not $chao -and (Cmd "execute in csmp:vazio if block $px 3 $pz csmp:chao_vazio") -match 'Test passed') { $chao = $true }
    } }
    Teste 'Dimensao Vazio gera o chao (csmp:chao_vazio)' $chao 'procurado em 9 pontos perto de 0,0'
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
    $null = Cmd 'execute in csmp:vazio run fill 40 4 40 56 30 56 minecraft:air'
    $null = Cmd 'execute in csmp:vazio run fill 40 3 40 56 3 56 minecraft:stone'
    $r = Cmd 'execute in csmp:vazio run place feature csmp:sculk_superficie 48 4 48'
    Teste 'Sculk de superficie (csmp:sculk_superficie) e colocado' ($r -match 'Placed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio run fill 36 3 36 60 6 60 minecraft:glass replace minecraft:sculk'
    Teste 'Sculk de superficie realmente espalha blocos de sculk' ($r -match 'Successfully filled') $r.Trim()
    $null = Cmd 'execute in csmp:vazio run fill 70 4 70 86 30 86 minecraft:air'
    $null = Cmd 'execute in csmp:vazio run fill 70 3 70 86 3 86 minecraft:stone'
    $r = Cmd 'execute in csmp:vazio run place feature csmp:sculk_caverna 78 4 78'
    Teste 'Sculk de caverna (csmp:sculk_caverna) e colocado' ($r -match 'Placed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio run fill 66 3 66 90 6 90 minecraft:glass replace minecraft:sculk'
    Teste 'Sculk de caverna realmente espalha blocos de sculk' ($r -match 'Successfully filled') $r.Trim()
    # --- NPCs do Vazio (estilo Yume Nikki): existem e nao sofrem dano
    foreach ($npc in 'sombra', 'crianca', 'sentinela') {
        $null = Cmd "summon csmp:$npc ~ ~ ~"
        $r = Cmd "execute if entity @e[type=csmp:$npc]"
        Teste "NPC do Vazio '$npc' pode ser invocado" ($r -match 'Test passed') $r.Trim()
        $null = Cmd "damage @e[type=csmp:$npc,limit=1] 5 minecraft:generic"
        $r = Cmd "execute if entity @e[type=csmp:$npc,nbt={Health:20.0f}]"
        Teste "NPC do Vazio '$npc' nao sofre dano" ($r -match 'Test passed') $r.Trim()
        $null = Cmd "kill @e[type=csmp:$npc]"
    }
    # --- Estruturas proprias do Vazio: cada uma e colocada e deixa seu bloco-assinatura
    $null = Cmd 'execute in csmp:vazio run forceload add 112 112 191 191'
    # espera os chunks gerarem de verdade (com as estruturas naturais) antes de colocar as do teste
    for ($t = 0; $t -lt 40; $t++) {
        Start-Sleep -Seconds 5
        $a = Cmd 'execute in csmp:vazio if loaded 112 3 112'
        $b = Cmd 'execute in csmp:vazio if loaded 191 3 191'
        if ($a -match 'Test passed' -and $b -match 'Test passed') { break }
    }
    Start-Sleep -Seconds 5
    $estruturas = @(@('estrutura_poste', 'minecraft:lantern'), @('estrutura_portas', 'minecraft:iron_door'),
        @('estrutura_casinha', 'minecraft:red_stained_glass_pane'), @('estrutura_escada', 'minecraft:polished_blackstone_brick_stairs'),
        @('estrutura_sala', 'minecraft:sea_lantern'), @('estrutura_monolito', 'minecraft:shroomlight'),
        @('estrutura_balanco', 'minecraft:chain'), @('estrutura_fonte', 'minecraft:nether_wart_block'),
        @('estrutura_cabana_torta', 'minecraft:soul_lantern'), @('estrutura_cabana_aberta', 'minecraft:red_stained_glass'),
        @('armadilha_poco', 'minecraft:pointed_dripstone'), @('armadilha_flechas', 'minecraft:dispenser'),
        @('armadilha_teia', 'minecraft:cobweb'), @('armadilha_fio', 'minecraft:tripwire_hook'))
    # geracao natural do Vazio: ruas e casas tem que aparecer sozinhas, no lugar que o plano diz
    foreach ($tipo in 'rua', 'casa') {
        $r = Cmd "csmp vazio $tipo"
        if ($r -match 'x=(-?\d+) z=(-?\d+)') {
            $vx = [int]$Matches[1]; $vz = [int]$Matches[2]
            $null = Cmd "execute in csmp:vazio run forceload add $($vx - 16) $($vz - 16) $($vx + 16) $($vz + 16)"
            for ($t = 0; $t -lt 40; $t++) {
                Start-Sleep -Seconds 3
                if ((Cmd "execute in csmp:vazio if loaded $($vx + 16) 3 $($vz + 16)") -match 'Test passed' -and
                    (Cmd "execute in csmp:vazio if loaded $($vx - 16) 3 $($vz - 16)") -match 'Test passed') { break }
            }
            Start-Sleep -Seconds 4
            $blocos = if ($tipo -eq 'rua') { @('minecraft:blackstone', 'minecraft:polished_blackstone', 'minecraft:light_gray_concrete') } else { @('minecraft:dark_oak_planks') }
            $achou = $false
            foreach ($bl in $blocos) {
                $r2 = Cmd "execute in csmp:vazio run fill $($vx - 1) 3 $($vz - 1) $($vx + 1) 3 $($vz + 1) minecraft:glass replace $bl"
                if ($r2 -match 'Successfully filled') { $achou = $true; break }
            }
            Teste "Vazio: $tipo gerada sozinha no lugar planejado ($vx, $vz)" $achou "$tipo em $vx $vz"
        } else { Teste "Vazio: comando acha $tipo" $false $r.Trim() }
    }
    for ($i = 0; $i -lt $estruturas.Count; $i++) {
        $nome = $estruturas[$i][0]; $assinatura = $estruturas[$i][1]
        $cx = 128 + 16 * ($i % 4); $cz = 128 + 16 * [int][math]::Floor($i / 4)
        $r = Cmd "execute in csmp:vazio run place feature csmp:$nome $($cx + 8) 4 $($cz + 8)"
        Teste "Estrutura do Vazio '$nome' e colocada" ($r -match 'Placed') $r.Trim()
        $r = Cmd "execute in csmp:vazio run fill $cx 1 $cz $($cx + 15) 40 $($cz + 15) minecraft:glass replace $assinatura"
        Teste "Estrutura do Vazio '$nome' deixa o bloco $assinatura" ($r -match 'Successfully filled') $r.Trim()
    }
    # --- Santuario: nas duas dimensoes, e o ritual de troca de terreno
    $null = Cmd 'execute in csmp:vazio run forceload add 176 176 239 239'
    $null = Cmd 'execute in minecraft:overworld run forceload add 176 176 239 239'
    Start-Sleep -Seconds 12
    $r = Cmd 'execute in csmp:vazio run place feature csmp:estatua_vazio_direto 208 4 208'
    Teste 'Circulo do ritual do Vazio e colocado' ($r -match 'Placed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio if block 208 3 208 csmp:sigilo[ativo=false]'
    Teste 'Simbolo do ritual no chao do Vazio (apagado)' ($r -match 'Test passed') $r.Trim()
    $r = Cmd 'execute in csmp:vazio if entity @e[type=csmp:estatua,x=208,y=4,z=208,distance=..6]'
    Teste 'No Vazio nao ha mais estatua (so o simbolo)' ($r -match 'Test failed') $r.Trim()
    $r = Cmd 'execute in minecraft:overworld positioned 208 64 208 positioned over world_surface run place feature csmp:estatua_superficie_direto ~ ~ ~'
    Teste 'Estatua do overworld (cemiterio) e colocada' ($r -match 'Placed') $r.Trim()
    $r = Cmd 'execute in minecraft:overworld positioned 208 64 208 positioned over world_surface if entity @e[type=csmp:estatua,distance=..4,nbt={Partes:0}]'
    Teste 'Estatua do overworld nasce quebrada (sem partes)' ($r -match 'Test passed') $r.Trim()
    $r = Cmd 'csmp rituais estado'
    Teste 'Rituais comecam escondidos (so admin revela)' ($r -match 'escondidos') $r.Trim()
    $r = Cmd 'csmp rituais revelar'
    Teste 'Admin revela os rituais' ($r -match 'Rituais revelados') $r.Trim()
    $r = Cmd 'csmp santuario trocar 208 208'
    Teste 'Ritual de troca inicia (acha a base nas duas dimensoes)' ($r -match 'Ritual iniciado') $r.Trim()
    Start-Sleep -Seconds 9
    $r = Cmd 'execute in minecraft:overworld run fill 209 -60 208 212 200 208 minecraft:glass replace minecraft:polished_blackstone'
    Teste 'Troca: o overworld passou a ter o piso preto do Vazio' ($r -match 'Successfully filled') $r.Trim()
    $r = Cmd 'execute in csmp:vazio run fill 209 0 208 212 60 208 minecraft:glass replace minecraft:stone_bricks'
    Teste 'Troca: o Vazio passou a ter a pedra do overworld' ($r -match 'Successfully filled') $r.Trim()
    # --- Floresta das Betulas Indocis: gera de verdade no overworld, com bioma, estatua e olhos nas betulas
    $r = Cmd 'csmp bosque'
    if ($r -match 'x=(-?\d+) z=(-?\d+)') {
        $bx = [int]$Matches[1]; $bz = [int]$Matches[2]
        Teste "Comando /csmp bosque informa o centro ($bx, $bz)" $true $r.Trim()
        $null = Cmd "execute in minecraft:overworld run forceload add $($bx - 48) $($bz - 48) $($bx + 47) $($bz + 47)"
        for ($t = 0; $t -lt 60; $t++) {
            Start-Sleep -Seconds 4
            $a = Cmd "execute in minecraft:overworld if loaded $($bx - 40) 70 $($bz - 40)"
            $b = Cmd "execute in minecraft:overworld if loaded $($bx + 40) 70 $($bz + 40)"
            if ($a -match 'Test passed' -and $b -match 'Test passed') { break }
        }
        Start-Sleep -Seconds 8
        $r = Cmd "execute in minecraft:overworld positioned $bx 70 $bz if biome ~ ~ ~ csmp:betulas_indoceis"
        Teste 'Bosque: o bioma Betulas Indocis foi pintado no centro' ($r -match 'Test passed') $r.Trim()
        $r = Cmd "execute in minecraft:overworld run fill $($bx - 1) -60 $($bz - 1) $($bx + 1) 150 $($bz + 1) minecraft:glass replace csmp:base_estatua"
        Teste 'Bosque: a base da estatua nasce no centro' ($r -match 'Successfully filled') $r.Trim()
        $olhos = 0
        foreach ($ox in ($bx - 48), ($bx - 16), ($bx + 16)) {
            foreach ($oz in ($bz - 48), ($bz - 16), ($bz + 16)) {
                foreach ($oy in 40, 72) {
                    $r = Cmd "execute in minecraft:overworld run fill $ox $oy $oz $($ox + 31) $($oy + 31) $($oz + 31) minecraft:glass replace csmp:betula_olho"
                    if ($r -match 'Successfully filled') { $olhos++ }
                }
            }
        }
        Teste "Bosque: ha troncos de betula com olho ($olhos de 18 caixas)" ($olhos -ge 1) "$olhos caixas"
    } else { Teste 'Comando /csmp bosque informa o centro' $false $r.Trim() }
    # --- Ars Nouveau: as construcoes do mod nao nascem mais; e o minerio sculk existe na geracao
    foreach ($est in 'ars_nouveau:hunter_wilden_den', 'ars_additions:arcane_library') {
        $r = Cmd "execute in minecraft:overworld run locate structure $est"
        Teste "Construcao do Ars '$est' nao nasce mais" ($r -notmatch 'The nearest') $r.Trim()
    }
    $null = Cmd 'execute in minecraft:overworld run forceload add 0 0 15 15'
    Start-Sleep -Seconds 4
    $null = Cmd 'execute in minecraft:overworld run fill 0 -40 0 15 -38 15 minecraft:stone'
    $r = Cmd 'execute in minecraft:overworld run place feature csmp:minerio_sculk 8 -39 8'
    Teste 'Minerio sculk (csmp:minerio_sculk) e colocado' ($r -match 'Placed') $r.Trim()
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
        Start-Sleep -Seconds 6   # deixa o servidor terminar de escrever o log de inicio (voice chat etc.)
        $null = Cmd ''
        $r = ''
        for ($tentativa = 0; $tentativa -lt 4 -and $r -notmatch '#caos has'; $tentativa++) {
            $r = Cmd 'scoreboard players get #caos csmp_caos'
        }
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
