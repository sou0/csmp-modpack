@echo off
echo Cria um mapa novo: APAGA o mundo antigo e limpa as folhas salvas. Feche o jogo e o servidor antes.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\mapa-novo.ps1"
pause
