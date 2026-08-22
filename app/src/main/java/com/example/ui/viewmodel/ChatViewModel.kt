package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AgentPreset
import com.example.data.model.AgentPresets
import com.example.data.model.FileAttachment
import com.example.data.remote.GemmaAgentEngine
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import com.example.util.TextToSpeechHelper
import com.example.util.ZipAndFileHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatRepository = ChatRepository(db.chatDao(), GemmaAgentEngine(application))
    private val settingsRepository = SettingsRepository(application)
    private val ttsHelper = TextToSpeechHelper(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    init {
        // Observe Settings
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        // Observe TTS state
        viewModelScope.launch {
            ttsHelper.currentUtteranceId.collectLatest { currentId ->
                _uiState.update { it.copy(isSpeakingMessageId = currentId) }
            }
        }

        // Observe Sessions
        viewModelScope.launch {
            chatRepository.allSessions.collectLatest { sessions ->
                _uiState.update { current ->
                    val active = current.activeSession?.let { active ->
                        sessions.find { it.id == active.id }
                    } ?: sessions.firstOrNull()

                    current.copy(
                        sessions = sessions,
                        activeSession = active
                    )
                }

                // If no session exists at startup, create one
                if (sessions.isEmpty()) {
                    createNewChat()
                } else if (_uiState.value.activeSession == null) {
                    selectSession(sessions.first().id)
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        val session = _uiState.value.sessions.find { it.id == sessionId }
        if (session != null) {
            val preset = AgentPresets.getById(session.agentPresetId)
            _uiState.update {
                it.copy(
                    activeSession = session,
                    currentPreset = preset,
                    isDrawerOpen = false
                )
            }
            subscribeToMessages(sessionId)
        }
    }

    private fun subscribeToMessages(sessionId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.getMessagesForSession(sessionId).collectLatest { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun createNewChat(presetId: String = _uiState.value.currentPreset.id) {
        viewModelScope.launch {
            val newSession = chatRepository.createNewSession(presetId)
            _uiState.update {
                it.copy(
                    activeSession = newSession,
                    currentPreset = AgentPresets.getById(presetId),
                    inputText = "",
                    pendingAttachments = emptyList(),
                    isDrawerOpen = false
                )
            }
            subscribeToMessages(newSession.id)
        }
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectPreset(preset: AgentPreset) {
        _uiState.update { it.copy(currentPreset = preset) }
        val active = _uiState.value.activeSession
        if (active != null) {
            viewModelScope.launch {
                chatRepository.updateSessionPreset(active.id, preset.id)
            }
        }
    }

    fun addAttachmentUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val attachment = ZipAndFileHelper.processUri(getApplication(), uri)
                _uiState.update {
                    it.copy(pendingAttachments = it.pendingAttachments + attachment)
                }
                showToast("Attached: ${attachment.name} (${attachment.sizeFormatted})")
            } catch (e: Exception) {
                showToast("Failed to attach file: ${e.localizedMessage}")
            }
        }
    }

    fun removePendingAttachment(attachment: FileAttachment) {
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments.filter { item -> item != attachment })
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        val attachments = state.pendingAttachments
        val activeSession = state.activeSession ?: return

        if (text.isBlank() && attachments.isEmpty()) return

        // Clear input and attachments immediately
        _uiState.update {
            it.copy(
                inputText = "",
                pendingAttachments = emptyList(),
                isSending = true
            )
        }

        viewModelScope.launch {
            try {
                chatRepository.sendMessage(
                    sessionId = activeSession.id,
                    userPrompt = text,
                    attachments = attachments,
                    agentPreset = state.currentPreset,
                    customInstruction = state.settings.customSystemPrompt,
                    isThinkingEnabled = state.settings.isThinkingEnabled,
                    language = state.settings.language,
                    activeModel = state.settings.activeModel
                )
            } catch (e: Exception) {
                showToast("Error generating response: ${e.localizedMessage}")
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun toggleSpeakMessage(content: String, messageId: String) {
        val lang = _uiState.value.settings.language
        if (!_uiState.value.settings.isTtsEnabled) {
            val toast = if (lang == "km") "មុខងារអានជាសំឡេងត្រូវបានបិទនៅក្នុងការកំណត់" else "TTS voice readout is disabled in Settings."
            showToast(toast)
            return
        }
        ttsHelper.speak(content, messageId)
    }

    fun openAttachmentDetails(attachment: FileAttachment) {
        _uiState.update { it.copy(activeDetailAttachment = attachment) }
    }

    fun closeAttachmentDetails() {
        _uiState.update { it.copy(activeDetailAttachment = null) }
    }

    fun setDrawerOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = isOpen) }
    }

    fun setSettingsOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = isOpen) }
    }

    fun deleteSession(sessionId: String) {
        val lang = _uiState.value.settings.language
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            showToast(if (lang == "km") "បានលុបការសន្ទនា" else "Conversation deleted")
            if (_uiState.value.activeSession?.id == sessionId) {
                val remaining = _uiState.value.sessions.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().id)
                } else {
                    createNewChat()
                }
            }
        }
    }

    fun pinSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.togglePinSession(sessionId)
        }
    }

    fun clearCurrentChat() {
        val active = _uiState.value.activeSession ?: return
        val lang = _uiState.value.settings.language
        viewModelScope.launch {
            db.chatDao().deleteMessagesForSession(active.id)
            showToast(if (lang == "km") "បានសម្អាតការសន្ទនា" else "Chat cleared")
        }
    }

    fun clearAllHistory() {
        val lang = _uiState.value.settings.language
        viewModelScope.launch {
            chatRepository.clearAllHistory()
            createNewChat()
            showToast(if (lang == "km") "បានលុបប្រវត្តិទាំងអស់រួចរាល់" else "All chat history cleared")
        }
    }

    fun shareCurrentChat() {
        val lang = _uiState.value.settings.language
        val messages = _uiState.value.messages
        if (messages.isEmpty()) {
            showToast(if (lang == "km") "មិនមានសារសម្រាប់ចែករំលែកទេ។" else "No messages to share.")
            return
        }

        val shareContent = buildString {
            appendLine(if (lang == "km") "=== ការសន្ទនាជាមួយ Gemma 4 ភ្នាក់ងារ AI ===" else "=== Conversation with Gemma 4 Agent AI ===")
            appendLine("${if (lang == "km") "របៀប" else "Persona"}: ${_uiState.value.currentPreset.getDisplayName(lang)}")
            appendLine()
            messages.forEach { msg ->
                val sender = if (msg.role == "user") (if (lang == "km") "អ្នក" else "User") else "Gemma 4"
                appendLine("[$sender]:")
                appendLine(msg.content)
                appendLine()
            }
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareContent)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(sendIntent, if (lang == "km") "ចែករំលែកការសន្ទនាតាម" else "Share Chat via").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<Application>().startActivity(shareIntent)
    }

    // Settings actions
    fun updateLanguage(language: String) = settingsRepository.updateLanguage(language)
    fun updateTheme(themeMode: String) = settingsRepository.updateTheme(themeMode)
    fun updateTextSize(textSize: String) = settingsRepository.updateTextSize(textSize)
    fun updateModel(model: String) = settingsRepository.updateActiveModel(model)
    fun updateThinking(enabled: Boolean) = settingsRepository.updateThinkingEnabled(enabled)
    fun updateTts(enabled: Boolean) = settingsRepository.updateTtsEnabled(enabled)
    fun updateCustomPrompt(prompt: String) = settingsRepository.updateCustomSystemPrompt(prompt)
    fun toggleGithub() = settingsRepository.toggleGithubConnection()

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            _uiState.update { current ->
                if (current.toastMessage == message) current.copy(toastMessage = null) else current
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}
