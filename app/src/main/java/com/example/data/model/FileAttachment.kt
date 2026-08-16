package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class FileAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sizeFormatted: String,
    val sizeBytes: Long = 0L,
    val mimeType: String = "application/octet-stream",
    val isZip: Boolean = false,
    val entryCount: Int = 0,
    val contentSummary: String = "",
    val textPreview: String = ""
)
