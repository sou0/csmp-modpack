# Raio distante (25-45 blocos): ameaca sem acertar o jogador de cara
summon marker ~ ~ ~ {Tags:["csmp_raio"]}
spreadplayers ~ ~ 25 45 false @e[type=marker,tag=csmp_raio,distance=..1,limit=1]
execute at @e[type=marker,tag=csmp_raio] run summon lightning_bolt ~ ~ ~
kill @e[type=marker,tag=csmp_raio]