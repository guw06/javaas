@echo off
chcp 65001 >nul
title J.A.R.V.I.S. — AI Assistant

echo.
echo ╔═══════════════════════════════════════════════════╗
echo ║     J.A.R.V.I.S. — Персональный ИИ-Ассистент    ║
echo ╚═══════════════════════════════════════════════════╝
echo.

:: Определяем путь к Java 21
set "JAVA_CMD=java"
if exist "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe"
)

:: Проверка Java
"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Java не найдена!
    echo Установите Java 21: https://adoptium.net
    pause
    exit /b 1
)

:: Проверка JAR файла
if exist "target\javaas-1.0-SNAPSHOT.jar" (
    echo [OK] Запуск J.A.R.V.I.S. ...
    "%JAVA_CMD%" -jar target\javaas-1.0-SNAPSHOT.jar
) else (
    echo [INFO] JAR не найден, сборка проекта...
    
    mvn -version >nul 2>&1
    if errorlevel 1 (
        echo [ОШИБКА] Maven не найден!
        echo Установите Maven: https://maven.apache.org
        pause
        exit /b 1
    )
    
    echo [INFO] Сборка...
    mvn clean package -q -DskipTests
    
    if exist "target\javaas-1.0-SNAPSHOT.jar" (
        echo [OK] Сборка завершена. Запуск...
        "%JAVA_CMD%" -jar target\javaas-1.0-SNAPSHOT.jar
    ) else (
        echo [ОШИБКА] Сборка не удалась
        pause
        exit /b 1
    )
)

pause
