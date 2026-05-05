@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

title AURA Personal Assistant

echo.
echo ===================================================
echo          AURA - Personal Assistant
echo ===================================================
echo.

set "JAVA_CMD=java"
if exist "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe"
)

"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java 21 was not found.
    echo Install Java 21 and run this file again.
    pause
    exit /b 1
)

echo [INFO] Java check passed.
call mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven was not found.
    echo Install Maven or add it to PATH.
    pause
    exit /b 1
)

echo [INFO] Maven check passed.
echo [INFO] Stopping old AURA if it is already running...
powershell -NoProfile -ExecutionPolicy Bypass -Command "for ($port = 8080; $port -le 8100; $port++) { try { $url = 'http://localhost:' + $port + '/ping'; $response = Invoke-WebRequest -UseBasicParsing $url -TimeoutSec 1; if ($response.Content.Trim() -eq 'pong') { $connection = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1; if ($connection) { $process = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $connection.OwningProcess) -ErrorAction SilentlyContinue; if ($process.CommandLine -like '*javaas-1.0-SNAPSHOT.jar*') { Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue; Start-Sleep -Seconds 1 } } } } catch {} }"

echo [INFO] Building fresh version...
call mvn clean package -q -DskipTests
if errorlevel 1 (
    echo [ERROR] Build failed.
    pause
    exit /b 1
)

if not exist "target\javaas-1.0-SNAPSHOT.jar" (
    echo [ERROR] JAR was not created: target\javaas-1.0-SNAPSHOT.jar
    pause
    exit /b 1
)

echo [OK] Starting AURA...
"%JAVA_CMD%" -jar target\javaas-1.0-SNAPSHOT.jar

pause
