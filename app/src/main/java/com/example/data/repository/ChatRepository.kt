package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.model.AgentPreset
import com.example.data.model.AgentPresets
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.FileAttachment
import com.example.data.model.LocalModel
import com.example.data.model.LocalModels
import com.example.data.remote.InferenceUnavailableException
import com.example.data.remote.LocalAgentEngine
import com.example.util.ZipAndFileHelper
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val agentEngine: LocalAgentEngine
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

    /**
     * Persists the user's turn, then streams a genuine on-device model response into the
     * assistant message as tokens arrive.
     *
     * If real inference is impossible the assistant message is marked as an error carrying the
     * actual reason — the app never substitutes a scripted reply.
     */
    suspend fun sendMessage(
        sessionId: String,
        userPrompt: String,
        attachments: List<FileAttachment>,
        agentPreset: AgentPreset,
        customInstruction: String,
        isThinkingEnabled: Boolean,
        language: String = "en",
        model: LocalModel = LocalModels.DEFAULT
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
            val session = chatDao.getSessionById(sessionId)
            if (session != null) {
                chatDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))
            }
        }

        // 2. Insert placeholder assistant message
        val assistantMessageId = UUID.randomUUID().toString()
        val placeholder = ChatMessage(
            id = assistantMessageId,
            sessionId = sessionId,
            role = "assistant",
            content = "",
            timestamp = System.currentTimeMillis() + 1,
            isThinking = true,
            status = "sending"
        )
        chatDao.insertMessage(placeholder)

        // 3. Stream real tokens from the on-device model
        val builder = StringBuilder()
        var lastFlush = 0L
        try {
            agentEngine.generate(
                userMessage = userPrompt,
                history = currentMessages.dropLast(1),
                attachments = attachments,
                agentPreset = agentPreset,
                customInstruction = customInstruction,
                isThinkingEnabled = isThinkingEnabled,
                language = language,
                model = model
            ).collect { token ->
                builder.append(token)
                val now = System.currentTimeMillis()
                // Persist incrementally so the UI streams, without hammering the database.
                if (now - lastFlush >= STREAM_FLUSH_INTERVAL_MS) {
                    lastFlush = now
                    val (thought, visible) = splitThinking(builder.toString())
                    chatDao.updateMessage(
                        placeholder.copy(
                            content = visible,
                            thinkingProcess = thought,
                            isThinking = true,
                            status = "sending"
                        )
                    )
                }
            }

            val (thought, visible) = splitThinking(builder.toString())
            chatDao.updateMessage(
                placeholder.copy(
                    content = visible.ifBlank {
                        "The model returned an empty response. Try rephrasing your question."
                    },
                    thinkingProcess = thought,
                    isThinking = false,
                    status = if (visible.isBlank()) "error" else "completed"
                )
            )
        } catch (e: InferenceUnavailableException) {
            chatDao.updateMessage(
                placeholder.copy(
                    content = e.message ?: "On-device inference is unavailable.",
                    isThinking = false,
                    status = "error"
                )
            )
            throw e
        } catch (e: Exception) {
            val partial = builder.toString()
            chatDao.updateMessage(
                placeholder.copy(
                    content = if (partial.isNotBlank()) partial else
                        "Generation stopped: ${e.message ?: "unknown error"}",
                    isThinking = false,
                    status = "error"
                )
            )
            throw e
        }
    }

    /** Separates an optional <thought>...</thought> preamble from the visible answer. */
    private fun splitThinking(raw: String): Pair<String, String> {
        val complete = Regex("<thought>([\\s\\S]*?)</thought>", RegexOption.IGNORE_CASE).find(raw)
        if (complete != null) {
            return complete.groupValues[1].trim() to raw.replace(complete.value, "").trim()
        }
        // Thought block still streaming: show it as reasoning, keep the answer empty for now.
        val open = Regex("<thought>([\\s\\S]*)$", RegexOption.IGNORE_CASE).find(raw)
        if (open != null) {
            return open.groupValues[1].trim() to raw.substring(0, open.range.first).trim()
        }
        return "" to raw.trim()
    }

    private companion object {
        const val STREAM_FLUSH_INTERVAL_MS = 120L
    }
}
