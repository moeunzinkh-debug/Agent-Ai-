package com.example.ui.viewmodel

import com.example.data.model.AgentPreset
import com.example.data.model.AgentPresets
import com.example.data.model.AppSettings
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.FileAttachment

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val activeSession: ChatSession? = null,
    val messages: List<ChatMessage> = emptyList(),
    val currentPreset: AgentPreset = AgentPresets.ALL.first(),
    val inputText: String = "",
    val pendingAttachments: List<FileAttachment> = emptyList(),
    val isSending: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isSpeakingMessageId: String? = null,
    val activeDetailAttachment: FileAttachment? = null,
    val toastMessage: String? = null,
    val settings: AppSettings = AppSettings()
)
