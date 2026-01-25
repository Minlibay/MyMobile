# Решение проблемы постоянной загрузки приложения

## Возможные причины:

1. **Gradle daemon завис** - процесс сборки застрял
2. **Проблемы с подключением к устройству/эмулятору**
3. **Ошибки компиляции, которые не отображаются**
4. **Проблемы с кешем Gradle**

## Решения (по порядку):

### 1. Остановить все процессы Gradle и перезапустить

**В Android Studio:**
- File → Invalidate Caches → Invalidate and Restart

**В терминале:**
```powershell
./gradlew.bat --stop
```

### 2. Очистить проект и пересобрать

**В Android Studio:**
- Build → Clean Project
- Build → Rebuild Project

**В терминале:**
```powershell
./gradlew.bat clean
./gradlew.bat :app:assembleDebug
```

### 3. Проверить подключение к устройству

**В Android Studio:**
- View → Tool Windows → Device Manager
- Убедитесь, что устройство/эмулятор запущен и подключен
- Попробуйте перезапустить эмулятор или переподключить устройство

**В терминале:**
```powershell
adb devices
```

### 4. Удалить кеш Gradle

```powershell
# Остановить Gradle daemon
./gradlew.bat --stop

# Удалить кеш (опционально)
Remove-Item -Recurse -Force $env:USERPROFILE\.gradle\caches
```

### 5. Проверить логи ошибок

**В Android Studio:**
- View → Tool Windows → Build
- View → Tool Windows → Logcat (после запуска приложения)

**В терминале:**
```powershell
# Проверить ошибки компиляции
./gradlew.bat :app:compileDebugKotlin --stacktrace

# Проверить ошибки сборки APK
./gradlew.bat :app:assembleDebug --stacktrace
```

### 6. Отменить запуск и попробовать снова

**В Android Studio:**
- Нажмите кнопку "Cancel" в окне "Background Tasks"
- Run → Stop 'app'
- Run → Run 'app' (или Shift+F10)

### 7. Перезапустить Android Studio

- File → Exit
- Запустите Android Studio снова
- File → Open → выберите проект

### 8. Проверить настройки запуска

**В Android Studio:**
- Run → Edit Configurations
- Убедитесь, что:
  - Module: app
  - Launch: Default Activity
  - Target: правильное устройство/эмулятор

## Если ничего не помогает:

1. Закройте Android Studio полностью
2. Удалите папку `.idea` в корне проекта (Android Studio пересоздаст её)
3. Откройте проект заново
4. Дождитесь индексации проекта
5. Попробуйте запустить снова

## Быстрая проверка работоспособности:

```powershell
# Установите JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Очистите и соберите проект
./gradlew.bat clean
./gradlew.bat :app:assembleDebug

# Если сборка успешна, APK находится в:
# app\build\outputs\apk\debug\app-debug.apk
```












