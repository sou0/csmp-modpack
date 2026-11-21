# Chamado a cada 1 s pelo loop: renova as tempestades (niveis 2 e 3)
# Intervalo: 300 s no nivel 2; 60 s no Apocalipse (mais tempestades formando perto dos jogadores)
execute unless score #clima csmp_cfg matches 1 run return 0
execute unless score #nivel_clima csmp_cfg matches 2.. run return 0
scoreboard players set #lim csmp_cfg 300
execute if score #nivel_clima csmp_cfg matches 3 run scoreboard players set #lim csmp_cfg 60
scoreboard players add #clima_t csmp_cfg 1
execute if score #clima_t csmp_cfg >= #lim csmp_cfg run scoreboard players set #clima_t csmp_cfg 0
execute if score #clima_t csmp_cfg matches 0 run function csmp_caos:clima/definir