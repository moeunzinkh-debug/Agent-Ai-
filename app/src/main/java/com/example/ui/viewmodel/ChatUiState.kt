package com.example.ui.viewmodel

import com.example.data.model.AgentPreset
import com.example.data.model.AgentPresets
import com.example.data.model.AppSettings
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.FileAttachment
import com.example.data.model.LocalModel
import com.example.data.model.LocalModels

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
    val settings: AppSettings = AppSettings(),

    // --- On-device model state ---
    /** The GGUF model currently selected for local inference. */
    val activeModel: LocalModel = LocalModels.DEFAULT,
    /** True once the full weights are present on the device. */
    val isModelDownloaded: Boolean = false,
    /** True while the weights are being fetched. */
    val isDownloadingModel: Boolean = false,
    /** Download progress, 0..100. */
    val downloadPercent: Int = 0,
    /** Bytes fetched so far, for the "412 MB / 800 MB" style label. */
    val downloadedBytes: Long = 0L,
    /** Last download error, shown verbatim to the user. */
    val downloadError: String? = null,
    /** True while llama.cpp is loading the weights into memory. */
    val isLoadingModel: Boolean = false,
    /** False when this device's CPU cannot run the native engine at all. */
    val isDeviceSupported: Boolean = true
)
