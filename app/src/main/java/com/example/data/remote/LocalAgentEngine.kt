package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.ModelRepository
import com.example.data.model.AgentPreset
import com.example.data.model.ChatMessage
import com.example.data.model.FileAttachment
import com.example.data.model.LocalModel
import com.example.data.model.LocalModels
import com.example.llama.LlamaBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Real on-device AI.
 *
 * Every token returned here is produced by llama.cpp running the downloaded GGUF weights on
 * this device's CPU. There are no canned answers and no scripted fallbacks: if inference
 * cannot run we surface the actual error instead of pretending to be an AI.
 */
class LocalAgentEngine(private val context: Context) {

    sealed interface EngineStatus {
        data object ModelMissing : EngineStatus
        data object Idle : EngineStatus
        data object LoadingModel : EngineStatus
        data object Ready : EngineStatus
        data object Generating : EngineStatus
        data class Error(val message: String) : EngineStatus
    }

    private val modelRepository = ModelRepository(context)

    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Idle)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    /** llama.cpp holds a single global context; serialise everything that touches it. */
    private val engineMutex = Mutex()

    private var loadedModelId: String? = null

    fun isModelAvailable(model: LocalModel = LocalModels.DEFAULT): Boolean =
        modelRepository.isDownloaded(model)

    /** False when this device's CPU has no compatible native library. */
    fun isDeviceSupported(): Boolean = LlamaBridge.isAvailable()

    fun refreshAvailability(model: LocalModel = LocalModels.DEFAULT) {
        if (!modelRepository.isDownloaded(model)) {
            _status.value = EngineStatus.ModelMissing
        } else if (_status.value is EngineStatus.ModelMissing) {
            _status.value = EngineStatus.Idle
        }
    }

    /**
     * Streams a genuine model response token by token.
     *
     * @throws InferenceUnavailableException when weights are missing or llama.cpp fails.
     */
    fun generate(
        userMessage: String,
        history: List<ChatMessage>,
        attachments: List<FileAttachment>,
        agentPreset: AgentPreset,
        customInstruction: String,
        language: String,
        model: LocalModel = LocalModels.DEFAULT
    ): Flow<String> = flow {
        val systemPrompt = buildSystemPrompt(agentPreset, customInstruction, language)
        val prompt = buildChatPrompt(systemPrompt, userMessage, history, attachments)

        engineMutex.withLock {
            ensureModelLoaded(model)
            _status.value = EngineStatus.Generating
            try {
                LlamaBridge.generate(prompt, PREDICT_TOKENS).collect { emit(it) }
                _status.value = EngineStatus.Ready
            } catch (e: CancellationException) {
                _status.value = EngineStatus.Ready
                throw e
            } catch (e: LlamaBridge.LlamaException) {
                _status.value = EngineStatus.Error(e.message ?: "Inference failed")
                throw InferenceUnavailableException(e.message ?: "Inference failed", e)
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed", e)
                _status.value = EngineStatus.Error(e.message ?: "Inference failed")
                throw InferenceUnavailableException(
                    "The model failed while generating a response: ${e.message ?: "unknown error"}",
                    e
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun ensureModelLoaded(model: LocalModel) {
        if (!LlamaBridge.isAvailable()) {
            _status.value = EngineStatus.Error("Unsupported CPU")
            throw InferenceUnavailableException(
                "This device's processor is not supported by the on-device AI engine " +
                    "(64-bit ARM is required)."
            )
        }
        if (!modelRepository.isDownloaded(model)) {
            _status.value = EngineStatus.ModelMissing
            throw InferenceUnavailableException(
                "${model.displayName} (${model.sizeLabel}) is not downloaded yet. " +
                    "Download it in Settings to start using the AI."
            )
        }
        if (loadedModelId == model.id && LlamaBridge.isModelLoaded()) return

        _status.value = EngineStatus.LoadingModel
        try {
            LlamaBridge.loadModel(
                modelFile = modelRepository.modelFile(model),
                contextLength = model.contextLength
            )
            loadedModelId = model.id
            _status.value = EngineStatus.Ready
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            loadedModelId = null
            _status.value = EngineStatus.Error(e.message ?: "Could not load the model")
            throw InferenceUnavailableException(
                e.message ?: "Could not load ${model.displayName}.", e
            )
        }
    }

    suspend fun unload() = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            runCatching { LlamaBridge.unload() }
                .onFailure { Log.w(TAG, "unload failed", it) }
            loadedModelId = null
            _status.value = EngineStatus.Idle
        }
    }

    private fun buildSystemPrompt(
        agentPreset: AgentPreset,
        customInstruction: String,
        language: String
    ): String = buildString {
        appendLine(agentPreset.systemPrompt)
        appendLine()
        if (language == "km") {
            appendLine(
                "LANGUAGE REQUIREMENT (MANDATORY): Answer in polite, natural, accurate Khmer " +
                    "(ភាសាខ្មែរ). Keep code, file paths and technical symbols inside Markdown code blocks."
            )
        } else {
            appendLine("LANGUAGE REQUIREMENT: Respond in clear, professional English using Markdown.")
        }
        if (customInstruction.isNotBlank()) {
            appendLine()
            appendLine("User custom instruction: $customInstruction")
        }
        appendLine()
        // Instant answering only: on a phone CPU a 1B model emits just a few tokens per second,
        // so narrating reasoning would make the app feel unresponsive.
        appendLine(
            "Answer directly and concisely. Do NOT narrate your reasoning step by step and " +
                "do NOT emit a <thought> block. Lead with the answer itself."
        )
    }.trim()

    /**
     * Builds the prompt using Llama 3.2's official chat template. Using the exact special
     * tokens the model was trained on matters a lot for a 1B model's output quality.
     */
    private fun buildChatPrompt(
        systemPrompt: String,
        userMessage: String,
        history: List<ChatMessage>,
        attachments: List<FileAttachment>
    ): String = buildString {
        append("<|begin_of_text|>")

        append("<|start_header_id|>system<|end_header_id|>\n\n")
        append(systemPrompt)
        append("<|eot_id|>")

        history
            .filter { it.content.isNotBlank() && it.status != "error" }
            .takeLast(HISTORY_TURNS)
            .forEach { msg ->
                val role = if (msg.role == "user") "user" else "assistant"
                append("<|start_header_id|>$role<|end_header_id|>\n\n")
                append(msg.content.take(MAX_HISTORY_CHARS))
                append("<|eot_id|>")
            }

        append("<|start_header_id|>user<|end_header_id|>\n\n")
        val attachmentContext = buildAttachmentContext(attachments)
        if (attachmentContext.isNotBlank()) {
            append("--- ATTACHED FILES ---\n")
            append(attachmentContext)
            append("--- END OF ATTACHMENTS ---\n\n")
        }
        append(
            userMessage.ifBlank {
                "Analyse the attached files and summarise what they contain."
            }
        )
        append("<|eot_id|>")

        append("<|start_header_id|>assistant<|end_header_id|>\n\n")
    }

    private fun buildAttachmentContext(attachments: List<FileAttachment>): String {
        if (attachments.isEmpty()) return ""
        return buildString {
            attachments.forEachIndexed { index, file ->
                appendLine("[Attachment ${index + 1}: ${file.name} (${file.sizeFormatted})]")
                if (file.isZip) {
                    appendLine("ZIP archive with ${file.entryCount} entries.")
                    appendLine(file.contentSummary.take(MAX_ATTACHMENT_CHARS))
                } else {
                    appendLine("Type: ${file.mimeType}")
                }
                if (file.textPreview.isNotBlank()) {
                    appendLine("Content preview:")
                    appendLine(file.textPreview.take(MAX_ATTACHMENT_CHARS))
                }
                appendLine()
            }
        }
    }

    private companion object {
        const val TAG = "LocalAgentEngine"
        const val PREDICT_TOKENS = 768

        // A 1B model with a 2K window fills up fast; keep history and attachments tight.
        const val HISTORY_TURNS = 4
        const val MAX_HISTORY_CHARS = 600
        const val MAX_ATTACHMENT_CHARS = 1500
    }
}

/**
 * Raised when a genuine model response cannot be produced. The UI surfaces this verbatim —
 * we never replace a failure with fabricated "AI" text.
 */
class InferenceUnavailableException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
