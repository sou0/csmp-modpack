# Raio a 10-25 blocos do jogador
summon marker ~ ~ ~ {Tags:["csmp_raio"]}
spreadplayers ~ ~ 10 25 false @e[type=marker,tag=csmp_raio,distance=..1,limit=1]
execute at @e[type=marker,tag=csmp_raio] run summon lightning_bolt ~ ~ ~
kill @e[type=marker,tag=csmp_raio]