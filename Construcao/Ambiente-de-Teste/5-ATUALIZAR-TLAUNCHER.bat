@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\atualizar.ps1" -Alvo tlauncher
pause
