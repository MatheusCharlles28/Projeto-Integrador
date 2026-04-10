@echo off
echo Compilando Carmel Sistema...
cd /d "%~dp0"
call mvn clean package -q
if %errorlevel% neq 0 (
    echo ERRO na compilacao! Verifique o console.
    pause
    exit /b 1
)
echo Build concluido! Iniciando sistema...
start javaw -jar target\carmelSystem.jar