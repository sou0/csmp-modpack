# Chamado a cada atualizacao do medidor. A tempestade so comeca a partir de 50% de caos.
# Desligado? (admin: config_clima {valor:0})
execute unless score #clima csmp_cfg matches 1 run return 0

# Nivel de clima: 0 normal (<50) | 2 tempestade (50-74) | 3 apocalipse (75+)
scoreboard players set #nv csmp_cfg 0
execute if score #caos csmp_caos matches 50..74 run scoreboard players set #nv csmp_cfg 2
execute if score #caos csmp_caos matches 75.. run scoreboard players set #nv csmp_cfg 3

# Mudou de nivel -> troca o clima na hora
execute unless score #nv csmp_cfg = #nivel_clima csmp_cfg run function csmp_caos:clima/mudou

# Raios (bem espacados):
#  50-74%  ~1 a cada 100 s por jogador, distantes (25-45 blocos)
#  75%+    ~1 a cada 25 s por jogador, proximos (10-25 blocos)
#  90%+    o dobro disso
execute if score #nv csmp_cfg matches 2 as @a[gamemode=!spectator] at @s if predicate csmp_caos:sorte_1 run function csmp_caos:clima/raio_longe
execute if score #nv csmp_cfg matches 3 as @a[gamemode=!spectator] at @s if predicate csmp_caos:sorte_4 run function csmp_caos:clima/raio
execute if score #caos csmp_caos matches 90.. as @a[gamemode=!spectator] at @s if predicate csmp_caos:sorte_4 run function csmp_caos:clima/raio