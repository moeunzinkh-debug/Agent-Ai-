package com.example.data.model

data class AppSettings(
    val language: String = "en", // "en" (English), "km" (Khmer)
    val themeMode: String = "dark", // "dark", "light", "system"
    val textSize: String = "medium", // "small", "medium", "large"
    val activeModel: String = "llama-3.2-1b-instruct", // id from LocalModels
    val customSystemPrompt: String = "",
    val responseMode: String = "instant", // "instant" or "thinking" (see ResponseMode)
    val isTtsEnabled: Boolean = true,
    val isGithubConnected: Boolean = true,
    val githubUsername: String = "agent-developer",
    val soundEffects: Boolean = true
)
