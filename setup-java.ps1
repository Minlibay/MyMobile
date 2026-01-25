# Скрипт для автоматической настройки JAVA_HOME для проекта Zhivoy
# Запустите этот скрипт в PowerShell перед работой с проектом

$javaHome = "C:\Program Files\Android\Android Studio\jbr"

if (Test-Path $javaHome) {
    $env:JAVA_HOME = $javaHome
    $env:Path = "$javaHome\bin;$env:Path"
    Write-Host "✓ JAVA_HOME установлен: $javaHome" -ForegroundColor Green
    
    # Проверка версии Java
    $javaVersion = & "$javaHome\bin\java.exe" -version 2>&1 | Select-Object -First 1
    Write-Host "✓ Версия Java: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "✗ JDK не найден по пути: $javaHome" -ForegroundColor Red
    Write-Host "Пожалуйста, установите Android Studio или укажите правильный путь к JDK" -ForegroundColor Yellow
    exit 1
}












