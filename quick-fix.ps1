# Быстрое исправление проблемы с запуском приложения

Write-Host "=== Быстрое исправление проблемы с запуском ===" -ForegroundColor Cyan

# 1. Установка JAVA_HOME
Write-Host "`n1. Установка JAVA_HOME..." -ForegroundColor Yellow
$javaHome = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $javaHome) {
    $env:JAVA_HOME = $javaHome
    $env:Path = "$javaHome\bin;$env:Path"
    Write-Host "   ✓ JAVA_HOME установлен" -ForegroundColor Green
} else {
    Write-Host "   ✗ JDK не найден: $javaHome" -ForegroundColor Red
    exit 1
}

# 2. Остановка Gradle daemon
Write-Host "`n2. Остановка Gradle daemon..." -ForegroundColor Yellow
./gradlew.bat --stop 2>&1 | Out-Null
Write-Host "   ✓ Gradle daemon остановлен" -ForegroundColor Green

# 3. Очистка проекта
Write-Host "`n3. Очистка проекта..." -ForegroundColor Yellow
./gradlew.bat clean 2>&1 | Out-Null
Write-Host "   ✓ Проект очищен" -ForegroundColor Green

# 4. Проверка подключения устройств
Write-Host "`n4. Проверка подключенных устройств..." -ForegroundColor Yellow
$devices = adb devices 2>&1 | Select-String -Pattern "device$"
if ($devices) {
    Write-Host "   ✓ Устройства найдены:" -ForegroundColor Green
    $devices | ForEach-Object { Write-Host "     $_" -ForegroundColor Gray }
} else {
    Write-Host "   ⚠ Устройства не найдены. Убедитесь, что эмулятор запущен или устройство подключено" -ForegroundColor Yellow
}

# 5. Компиляция проекта
Write-Host "`n5. Компиляция проекта..." -ForegroundColor Yellow
$compileResult = ./gradlew.bat :app:compileDebugKotlin 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✓ Компиляция успешна" -ForegroundColor Green
} else {
    Write-Host "   ✗ Ошибки компиляции:" -ForegroundColor Red
    $compileResult | Select-String -Pattern "(error|Error|ERROR)" | ForEach-Object { Write-Host "     $_" -ForegroundColor Red }
    exit 1
}

# 6. Сборка APK
Write-Host "`n6. Сборка APK..." -ForegroundColor Yellow
$buildResult = ./gradlew.bat :app:assembleDebug 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✓ APK успешно собран" -ForegroundColor Green
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = (Get-Item $apkPath).Length / 1MB
        Write-Host "     Размер APK: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Gray
    }
} else {
    Write-Host "   ✗ Ошибки сборки:" -ForegroundColor Red
    $buildResult | Select-String -Pattern "(error|Error|ERROR|FAILED)" | ForEach-Object { Write-Host "     $_" -ForegroundColor Red }
    exit 1
}

Write-Host "`n=== Готово! ===" -ForegroundColor Cyan
Write-Host "Теперь попробуйте запустить приложение из Android Studio" -ForegroundColor Green













