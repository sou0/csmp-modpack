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

# Manifesto para o instalador/atualizador (Construcao\Instalador): lista cada arquivo com tamanho e SHA-1.
# O CSMP.exe baixa este arquivo do GitHub e compara com a pasta do jogo de cada um.
$ver = Get-Content (Join-Path $Raiz 'Instalador\versao.txt') -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $ver) { $ver = '1' }
$linhas = New-Object System.Collections.Generic.List[string]
# so entra o que vai para o GitHub: o que o .gitignore ignora (cache/bibliotecas do MCEF etc.) fica de fora
$todos = foreach ($pasta in 'mods', 'config', 'resourcepacks') { Get-ChildItem "$Pronto\$pasta" -Recurse -File }
$nomeRaiz = Split-Path $Pronto -Leaf
$ignorados = @{}
git -c core.quotepath=off -C (Split-Path $Pronto -Parent) ls-files --others --ignored --exclude-standard -- $nomeRaiz |
    ForEach-Object { $ignorados[$_] = $true }
$todos | Sort-Object FullName | ForEach-Object {
        $rel = $_.FullName.Substring($Pronto.Length + 1).Replace('\', '/')
        if ($ignorados.ContainsKey("$nomeRaiz/$rel")) { return }
        $h = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA1).Hash.ToLower()
        $relJson = $rel.Replace('"', '\"')
        $linhas.Add("    {`"c`": `"$relJson`", `"t`": $($_.Length), `"h`": `"$h`"}")
}
$json = "{`n  `"versao`": `"$(Get-Date -Format 'yyyy-MM-dd HH:mm')`",`n  `"minecraft`": `"1.21.1`",`n  `"neoforge`": `"21.1.267`",`n" +
        "  `"instalador`": $ver,`n  `"arquivos`": [`n" + ($linhas -join ",`n") + "`n  ]`n}`n"
[IO.File]::WriteAllText("$Pronto\csmp-manifesto.json", $json, (New-Object Text.UTF8Encoding($false)))
Write-Host "Pasta pronta atualizada: $Pronto  ($qtd mods, $($linhas.Count) arquivos no manifesto)"
$global:LASTEXITCODE = 0
