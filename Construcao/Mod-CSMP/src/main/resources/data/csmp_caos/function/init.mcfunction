# Objetivos
scoreboard objectives add csmp_caos dummy
scoreboard objectives add csmp_cfg dummy
scoreboard objectives add csmp_kills playerKillCount

# Valores padrao (so definidos se ainda nao existirem)
execute unless score #caos csmp_caos = #caos csmp_caos run scoreboard players set #caos csmp_caos 0
execute unless score #contador csmp_caos = #contador csmp_caos run scoreboard players set #contador csmp_caos 0
execute unless score #ganho csmp_cfg = #ganho csmp_cfg run scoreboard players set #ganho csmp_cfg 10
execute unless score #queda csmp_cfg = #queda csmp_cfg run scoreboard players set #queda csmp_cfg 1
execute unless score #segundos csmp_cfg = #segundos csmp_cfg run scoreboard players set #segundos csmp_cfg 5
execute unless score #clima csmp_cfg = #clima csmp_cfg run scoreboard players set #clima csmp_cfg 1
scoreboard players set #nivel_clima csmp_cfg -1
scoreboard players set #clima_t csmp_cfg 0

# Barra (boss bar) visivel para todos
execute store success score #existe csmp_cfg run bossbar get csmp_caos:medidor max
execute if score #existe csmp_cfg matches 0 run bossbar add csmp_caos:medidor {"text":"Medidor de Caos"}
bossbar set csmp_caos:medidor max 100
bossbar set csmp_caos:medidor style progress
bossbar set csmp_caos:medidor visible true

function csmp_caos:atualizar
schedule function csmp_caos:loop 20t replace