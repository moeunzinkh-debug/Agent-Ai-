package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.FileAttachment
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object ZipAndFileHelper {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, FileAttachment::class.java)
    private val adapter = moshi.adapter<List<FileAttachment>>(listType)

    fun serializeAttachments(attachments: List<FileAttachment>): String {
        return try {
            adapter.toJson(attachments)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeAttachments(json: String): List<FileAttachment> {
        if (json.isBlank()) return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun processUri(context: Context, uri: Uri): FileAttachment = withContext(Dispatchers.IO) {
        var fileName = "attachment"
        var fileSize = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        val isZip = fileName.endsWith(".zip", ignoreCase = true) ||
                fileName.endsWith(".jar", ignoreCase = true) ||
                fileName.endsWith(".aar", ignoreCase = true)

        val sizeFormatted = formatFileSize(fileSize)

        if (isZip) {
            processZipFile(context, uri, fileName, fileSize, sizeFormatted)
        } else {
            processRegularFile(context, uri, fileName, fileSize, sizeFormatted)
        }
    }

    private fun processZipFile(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        sizeFormatted: String
    ): FileAttachment {
        val entryNames = mutableListOf<String>()
        val textSnippets = StringBuilder()
        var totalEntries = 0

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null && totalEntries < 100) {
                        totalEntries++
                        val name = entry.name
                        entryNames.add(name)

                        // If it's a code/text file and not too large, read sample lines
                        if (!entry.isDirectory && isReadableCodeOrText(name) && textSnippets.length < 3000) {
                            try {
                                val reader = BufferedReader(InputStreamReader(zis))
                                val snippet = StringBuilder()
                                var lineCount = 0
                                var line = reader.readLine()
                                while (line != null && lineCount < 25) {
                                    snippet.appendLine(line)
                                    lineCount++
                                    line = reader.readLine()
                                }
                                if (snippet.isNotEmpty()) {
                                    textSnippets.appendLine("--- File: $name ---")
                                    textSnippets.appendLine(snippet.toString())
                                    textSnippets.appendLine()
                                }
                            } catch (e: Exception) {
                                // Skip read error
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            textSnippets.appendLine("Error reading ZIP structure: ${e.message}")
        }

        val summary = buildString {
            appendLine("ZIP Archive contains $totalEntries files/directories.")
            appendLine("Hierarchy preview:")
            entryNames.take(20).forEach { appendLine(" - $it") }
            if (entryNames.size > 20) {
                appendLine(" ... and ${entryNames.size - 20} more files.")
            }
        }

        return FileAttachment(
            name = fileName,
            sizeFormatted = sizeFormatted,
            sizeBytes = fileSize,
            mimeType = "application/zip",
            isZip = true,
            entryCount = totalEntries,
            contentSummary = summary,
            textPreview = textSnippets.toString()
        )
    }

    private fun processRegularFile(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        sizeFormatted: String
    ): FileAttachment {
        val contentPreview = StringBuilder()
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

        if (isReadableCodeOrText(fileName) || mimeType.startsWith("text/")) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var lineCount = 0
                        var line = reader.readLine()
                        while (line != null && lineCount < 100 && contentPreview.length < 4000) {
                            contentPreview.appendLine(line)
                            lineCount++
                            line = reader.readLine()
                        }
                    }
                }
            } catch (e: Exception) {
                contentPreview.append("Unable to preview text: ${e.message}")
            }
        }

        return FileAttachment(
            name = fileName,
            sizeFormatted = sizeFormatted,
            sizeBytes = fileSize,
            mimeType = mimeType,
            isZip = false,
            entryCount = 1,
            contentSummary = if (contentPreview.isNotEmpty()) "Text document / source file preview (${contentPreview.lines().size} lines)" else "Binary / document attachment",
            textPreview = contentPreview.toString()
        )
    }

    private fun isReadableCodeOrText(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".kt") || lower.endsWith(".java") ||
                lower.endsWith(".py") || lower.endsWith(".js") ||
                lower.endsWith(".ts") || lower.endsWith(".json") ||
                lower.endsWith(".xml") || lower.endsWith(".html") ||
                lower.endsWith(".css") || lower.endsWith(".md") ||
                lower.endsWith(".txt") || lower.endsWith(".gradle") ||
                lower.endsWith(".kts") || lower.endsWith(".properties") ||
                lower.endsWith(".c") || lower.endsWith(".cpp") ||
                lower.endsWith(".h") || lower.endsWith(".sh") ||
                lower.endsWith(".yaml") || lower.endsWith(".yml") ||
                lower.endsWith(".toml") || lower.endsWith(".sql")
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.1f MB", mb)
    }
}
