package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.model.AgentPreset
import com.example.data.model.AgentPresets
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.FileAttachment
import com.example.data.remote.GemmaAgentEngine
import com.example.util.ZipAndFileHelper
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val agentEngine: GemmaAgentEngine = GemmaAgentEngine()
) {

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(presetId: String = "general"): ChatSession {
        val preset = AgentPresets.getById(presetId)
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "New Chat (${preset.name})",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            agentPresetId = presetId
        )
        chatDao.insertSession(session)
        return session
    }

    suspend fun updateSessionTitle(sessionId: String, newTitle: String) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun updateSessionPreset(sessionId: String, presetId: String) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(agentPresetId = presetId, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun togglePinSession(sessionId: String) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(isPinned = !session.isPinned))
        }
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun clearAllHistory() {
        chatDao.clearAllMessages()
        chatDao.clearAllSessions()
    }

    suspend fun searchMessages(query: String): List<ChatMessage> {
        return chatDao.searchMessages(query)
    }

    suspend fun sendMessage(
        sessionId: String,
        userPrompt: String,
        attachments: List<FileAttachment>,
        agentPreset: AgentPreset,
        customInstruction: String,
        isThinkingEnabled: Boolean,
        language: String = "en"
    ) {
        val timestamp = System.currentTimeMillis()
        val attachmentsJson = ZipAndFileHelper.serializeAttachments(attachments)

        // 1. Insert user message
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            content = userPrompt,
            timestamp = timestamp,
            attachmentsJson = attachmentsJson,
            status = "completed"
        )
        chatDao.insertMessage(userMessage)

        // Auto-update session title if it's the first message
        val currentMessages = chatDao.getMessagesListForSession(sessionId)
        if (currentMessages.size <= 1) {
            val generatedTitle = if (userPrompt.isNotBlank()) {
                val preview = userPrompt.take(35).replace("\n", " ")
                if (userPrompt.length > 35) "$preview..." else preview
            } else if (attachments.isNotEmpty()) {
                "Analysis: ${attachments.first().name}"
            } else {
                if (language == "km") "ការសន្ទនា" else "Conversation"
            }
            updateSessionTitle(sessionId, generatedTitle)
        } else {
            // Touch updatedAt
            val session = chatDao.getSessionById(sessionId)
            if (session != null) {
                chatDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))
            }
        }

        // 2. Insert placeholder assistant message with thinking status
        val assistantMessageId = UUID.randomUUID().toString()
        val placeholderAssistant = ChatMessage(
            id = assistantMessageId,
            sessionId = sessionId,
            role = "assistant",
            content = "",
            timestamp = System.currentTimeMillis() + 1,
            isThinking = true,
            status = "sending"
        )
        chatDao.insertMessage(placeholderAssistant)

        // 3. Call AI Agent Engine
        val result = agentEngine.generateAgentResponse(
            userMessage = userPrompt,
            history = currentMessages,
            attachments = attachments,
            agentPreset = agentPreset,
            customInstruction = customInstruction,
            isThinkingEnabled = isThinkingEnabled,
            language = language
        )

        // 4. Update assistant message with response
        val finalAssistantMessage = placeholderAssistant.copy(
            content = result.responseText,
            isThinking = false,
            thinkingProcess = result.thinkingProcess,
            status = if (result.isSuccess) "completed" else "error"
        )
        chatDao.updateMessage(finalAssistantMessage)
    }
}
