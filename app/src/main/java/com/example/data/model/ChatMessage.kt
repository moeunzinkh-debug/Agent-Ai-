package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false,
    val thinkingProcess: String = "",
    val attachmentsJson: String = "[]",
    val status: String = "completed" // "sending", "completed", "error"
)
