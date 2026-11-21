# Limita entre 0 e 100
execute if score #caos csmp_caos matches 101.. run scoreboard players set #caos csmp_caos 100
execute if score #caos csmp_caos matches ..-1 run scoreboard players set #caos csmp_caos 0

# Valor da barra e jogadores que a veem (todos)
execute store result bossbar csmp_caos:medidor value run scoreboard players get #caos csmp_caos
bossbar set csmp_caos:medidor players @a

# Nivel (cor + titulo)
execute if score #caos csmp_caos matches ..24 run function csmp_caos:nivel/calmo
execute if score #caos csmp_caos matches 25..49 run function csmp_caos:nivel/tenso
execute if score #caos csmp_caos matches 50..74 run function csmp_caos:nivel/caotico
execute if score #caos csmp_caos matches 75.. run function csmp_caos:nivel/apocalipse

# Clima (Create: Skys) acompanha o nivel
function csmp_caos:clima/aplicar
