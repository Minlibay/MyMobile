@echo off
REM Скрипт для автоматической настройки JAVA_HOME для проекта Zhivoy
REM Запустите этот скрипт в командной строке перед работой с проектом

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if exist "%JAVA_HOME%\bin\java.exe" (
    echo ✓ JAVA_HOME установлен: %JAVA_HOME%
    "%JAVA_HOME%\bin\java.exe" -version
) else (
    echo ✗ JDK не найден по пути: %JAVA_HOME%
    echo Пожалуйста, установите Android Studio или укажите правильный путь к JDK
    exit /b 1
)












