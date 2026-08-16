package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gemma_agent_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            language = prefs.getString("language", "en") ?: "en",
            themeMode = prefs.getString("theme_mode", "dark") ?: "dark",
            textSize = prefs.getString("text_size", "medium") ?: "medium",
            activeModel = prefs.getString("active_model", "gemma-4-flash") ?: "gemma-4-flash",
            customSystemPrompt = prefs.getString("custom_system_prompt", "") ?: "",
            isThinkingEnabled = prefs.getBoolean("is_thinking_enabled", true),
            isTtsEnabled = prefs.getBoolean("is_tts_enabled", true),
            isGithubConnected = prefs.getBoolean("is_github_connected", true),
            githubUsername = prefs.getString("github_username", "gemma-developer") ?: "gemma-developer",
            soundEffects = prefs.getBoolean("sound_effects", true)
        )
    }

    fun updateLanguage(language: String) {
        prefs.edit().putString("language", language).apply()
        _settings.update { it.copy(language = language) }
    }

    fun updateTheme(themeMode: String) {
        prefs.edit().putString("theme_mode", themeMode).apply()
        _settings.update { it.copy(themeMode = themeMode) }
    }

    fun updateTextSize(textSize: String) {
        prefs.edit().putString("text_size", textSize).apply()
        _settings.update { it.copy(textSize = textSize) }
    }

    fun updateActiveModel(model: String) {
        prefs.edit().putString("active_model", model).apply()
        _settings.update { it.copy(activeModel = model) }
    }

    fun updateCustomSystemPrompt(prompt: String) {
        prefs.edit().putString("custom_system_prompt", prompt).apply()
        _settings.update { it.copy(customSystemPrompt = prompt) }
    }

    fun updateThinkingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_thinking_enabled", enabled).apply()
        _settings.update { it.copy(isThinkingEnabled = enabled) }
    }

    fun updateTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_tts_enabled", enabled).apply()
        _settings.update { it.copy(isTtsEnabled = enabled) }
    }

    fun toggleGithubConnection() {
        val newStatus = !_settings.value.isGithubConnected
        prefs.edit().putBoolean("is_github_connected", newStatus).apply()
        _settings.update { it.copy(isGithubConnected = newStatus) }
    }

    fun updateGithubUsername(username: String) {
        prefs.edit().putString("github_username", username).apply()
        _settings.update { it.copy(githubUsername = username) }
    }
}
