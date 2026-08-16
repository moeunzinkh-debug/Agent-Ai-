package com.example.data.model

data class AppSettings(
    val language: String = "en", // "en" (English), "km" (Khmer)
    val themeMode: String = "dark", // "dark", "light", "system"
    val textSize: String = "medium", // "small", "medium", "large"
    val activeModel: String = "gemma-4-flash", // "gemma-4-flash", "gemma-4-pro", "gemini-3.5-flash"
    val customSystemPrompt: String = "",
    val isThinkingEnabled: Boolean = true,
    val isTtsEnabled: Boolean = true,
    val isGithubConnected: Boolean = true,
    val githubUsername: String = "gemma-developer",
    val soundEffects: Boolean = true
)
