# Soma (kills x ganho) ao caos e zera o contador do jogador
scoreboard players operation #tmp csmp_caos = @s csmp_kills
scoreboard players operation #tmp csmp_caos *= #ganho csmp_cfg
scoreboard players operation #caos csmp_caos += #tmp csmp_caos
scoreboard players set @s csmp_kills 0
function csmp_caos:atualizar