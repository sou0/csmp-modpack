# Nivel 0: devolve o clima ao ciclo normal do Minecraft (limpa a tempestade forcada)
execute if score #nivel_clima csmp_cfg matches 0 run weather clear
execute if score #nivel_clima csmp_cfg matches 0 run gamerule doWeatherCycle true
# Niveis 2 e 3: o caos manda no clima (o Create: Skys forma tempestades locais perto dos jogadores)
execute if score #nivel_clima csmp_cfg matches 2.. run gamerule doWeatherCycle false
execute if score #nivel_clima csmp_cfg matches 2 run weather thunder 1000000
execute if score #nivel_clima csmp_cfg matches 3 run weather thunder 1000000