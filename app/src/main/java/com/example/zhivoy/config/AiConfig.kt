package com.example.zhivoy.config

/**
 * Настройки AI ВНУТРИ проекта.
 *
 * Важно:
 * - API ключ НЕ хранится в БД и НЕ хранится в этом файле.
 * - API ключ задаётся через Gradle BuildConfig (см. `app/build.gradle.kts`).
 */
object AiConfig {
    /** OpenRouter model id (Qwen2.5). */
    const val BASE_URL: String = "https://openrouter.ai/api/v1/"
    const val MODEL: String = "qwen/qwen-2.5-vl-7b-instruct:free"

    /**
     * Текст первого сообщения в чате (UI), чтобы диалог начинался вопросом.
     * Это не системный промпт модели.
     */
    const val INITIAL_ASSISTANT_MESSAGE: String =
        "Привет! Какое блюдо вы сегодня ели? Можете прислать фото или описать текстом."

    /**
     * System prompt.
     * Требование: отвечать строго JSON-объектом, чтобы мы могли распарсить ответ.
     */
    val SYSTEM_PROMPT: String = """
        Ты — помощник по питанию. Твоя задача: по тексту и/или фото определить название блюда
        и примерную калорийность.

        ВАЖНО: в ответе НЕ задавай вопросов и НЕ добавляй пояснений.
        Верни ТОЛЬКО JSON-объект строго такого вида:
        {"title": "Название", "calories": 123}

        Значение calories — целое число.
        Язык названия блюда — русский.
    """.trimIndent()
}
