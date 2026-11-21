@echo off
echo Teste de fumaca do servidor (leva alguns minutos, nao precisa abrir o jogo)...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\servidor-testar.ps1"
pause
