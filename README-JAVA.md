# Настройка Java для проекта Zhivoy

## Автоматическая настройка

Проект автоматически использует JDK из Android Studio. Если вы видите ошибки о JDK 17+, выполните одно из следующих действий:

### Вариант 1: Использовать скрипт настройки (рекомендуется)

**PowerShell:**
```powershell
.\setup-java.ps1
```

**Командная строка:**
```cmd
setup-java.bat
```

### Вариант 2: Настройка вручную

Установите переменную окружения `JAVA_HOME`:

**PowerShell:**
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

**Командная строка:**
```cmd
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
```

### Вариант 3: Постоянная настройка (для всей системы)

1. Откройте "Система" → "Дополнительные параметры системы"
2. Нажмите "Переменные среды"
3. Создайте новую переменную `JAVA_HOME` со значением: `C:\Program Files\Android\Android Studio\jbr`
4. Добавьте `%JAVA_HOME%\bin` в переменную `Path`

## Проверка настройки

Проверьте версию Java:
```cmd
java -version
```

Должна быть версия 17 или выше.

## Примечания

- Проект настроен на использование Java 17 в `app/build.gradle.kts`
- Gradle автоматически использует JDK из `gradle.properties` (если установлен)
- `gradlew.bat` автоматически ищет JDK из Android Studio, если `JAVA_HOME` не установлен
- VS Code настроен через `.vscode/settings.json` для использования правильного JDK




