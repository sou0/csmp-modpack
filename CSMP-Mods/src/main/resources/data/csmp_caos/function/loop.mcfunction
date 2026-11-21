# Roda a cada 1 segundo
scoreboard players add #contador csmp_caos 1
execute if score #contador csmp_caos >= #segundos csmp_cfg run function csmp_caos:decair
function csmp_caos:atualizar
function csmp_caos:clima/reaplicar
schedule function csmp_caos:loop 20t replace