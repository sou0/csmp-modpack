# Chamado ao ligar/desligar o clima pelo caos. 0 = clima normal do Minecraft sempre.
execute unless score #clima csmp_cfg matches 1 run gamerule doWeatherCycle true
scoreboard players set #nivel_clima csmp_cfg -1
function csmp_caos:atualizar