# Улучшения UI/UX: Индикаторы загрузки и состояния

## Реализованные компоненты

### 1. Skeleton Screens
- `SkeletonCard()` - скелетон для обычных карточек
- `SkeletonStatCard()` - скелетон для статистических карточек

**Использование:**
```kotlin
if (isLoading) {
    repeat(3) {
        SkeletonCard()
    }
} else {
    // Контент
}
```

### 2. Индикаторы загрузки
- `LoadingIndicator()` - центральный индикатор загрузки
- `ButtonLoadingIndicator()` - индикатор для кнопок
- `LoadingContent()` - обертка для контента с индикатором загрузки

**Использование:**
```kotlin
LoadingContent(isLoading = isLoading) {
    // Контент
}
```

### 3. Snackbar уведомления
- `ModernSnackbarHost()` - хост для отображения уведомлений
- `showSuccess()` - показать успешное сообщение
- `showError()` - показать сообщение об ошибке

**Использование:**
```kotlin
val snackbarHostState = remember { SnackbarHostState() }

// В Scaffold или Box
ModernSnackbarHost(
    snackbarHostState = snackbarHostState,
    modifier = Modifier.align(Alignment.BottomCenter)
)

// Показать уведомление
scope.launch {
    snackbarHostState.showSuccess("Данные сохранены!")
}
```

### 4. Pull-to-Refresh
Реализован в `HomeScreen`:
```kotlin
val pullToRefreshState = rememberPullToRefreshState()

Box(
    modifier = Modifier
        .fillMaxSize()
        .nestedScroll(pullToRefreshState.nestedScrollConnection)
) {
    // Контент
    PullToRefreshContainer(
        state = pullToRefreshState,
        modifier = Modifier.align(Alignment.TopCenter)
    )
}
```

### 5. Улучшенные кнопки
`ModernButton` теперь поддерживает индикатор загрузки:
```kotlin
ModernButton(
    text = "Сохранить",
    onClick = { /* ... */ },
    isLoading = loading
)
```

## Интеграция в экраны

### MainScreen
- ✅ Добавлен `SnackbarHost` для глобальных уведомлений
- ✅ Уведомление при выходе из аккаунта

### HomeScreen
- ✅ Добавлен Pull-to-Refresh
- ⏳ Можно добавить Snackbar уведомления для успешных действий (добавление еды, веса и т.д.)

### LoginScreen / RegisterScreen
- ✅ Индикаторы загрузки в кнопках

## Следующие шаги (опционально)

1. Добавить Snackbar уведомления в HomeScreen для:
   - Добавления еды
   - Добавления веса
   - Добавления книги
   - Выполнения квестов

2. Использовать Skeleton screens при первой загрузке данных

3. Добавить обработку ошибок сети с красивыми экранами

4. Использовать `AsyncContent` для асинхронных операций










