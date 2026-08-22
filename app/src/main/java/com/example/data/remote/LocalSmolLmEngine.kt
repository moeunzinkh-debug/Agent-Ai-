package com.example.data.remote

import android.content.Context
import android.net.Uri
import com.example.data.model.AiModels
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.nehuatl.llamacpp.LlamaHelper

/** Downloads, validates, loads, and runs the SmolLM GGUF entirely on-device. */
class LocalSmolLmEngine(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val events = MutableSharedFlow<LlamaHelper.LLMEvent>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val helper = LlamaHelper(appContext.contentResolver, scope, events)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .build()
    private val modelFile = File(appContext.filesDir, "models/${AiModels.SMOLLM_DOWNLOAD_FILENAME}")

    @Volatile private var loaded = false
    @Volatile private var loading: CompletableDeferred<Unit>? = null
    @Volatile private var generation: CompletableDeferred<String>? = null

    init {
        scope.launch {
            events.collect { event ->
                when (event) {
                    is LlamaHelper.LLMEvent.Done -> generation?.complete(event.fullText)
                    is LlamaHelper.LLMEvent.Error -> {
                        val error = IllegalStateException(event.message)
                        loading?.completeExceptionally(error)
                        generation?.completeExceptionally(error)
                    }
                    else -> Unit
                }
            }
        }
    }

    suspend fun generate(prompt: String): String {
        ensureLoaded()
        check(generation?.isActive != true) { "A local generation is already running" }

        val result = CompletableDeferred<String>()
        generation = result
        return try {
            helper.predict(prompt)
            result.await().trim()
        } finally {
            generation = null
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        loading?.let { return it.await() }

        val pending = CompletableDeferred<Unit>()
        loading = pending
        try {
            ensureModelDownloaded()
            helper.load(Uri.fromFile(modelFile).toString(), contextLength = 2048) {
                loaded = true
                pending.complete(Unit)
            }
            pending.await()
        } finally {
            loading = null
        }
    }

    private fun ensureModelDownloaded() {
        if (modelFile.isFile && modelFile.length() == AiModels.SMOLLM_SIZE_BYTES) return

        modelFile.parentFile?.mkdirs()
        modelFile.delete()
        val partial = File(modelFile.parentFile, "${modelFile.name}.part")
        partial.delete()

        val revision = "3e9ad43ac27edce8db1d6aad1229940a2c81a37b"
        val url = "https://huggingface.co/${AiModels.SMOLLM_REPOSITORY}/resolve/$revision/${AiModels.SMOLLM_DOWNLOAD_FILENAME}"
        val request = Request.Builder().url(url).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Model download failed (HTTP ${response.code})" }
                val body = checkNotNull(response.body) { "Model download returned no data" }
                partial.outputStream().buffered().use { output -> body.byteStream().use { it.copyTo(output) } }
            }
            check(partial.length() == AiModels.SMOLLM_SIZE_BYTES) {
                "Downloaded model has an unexpected size"
            }
            check(sha256(partial) == AiModels.SMOLLM_SHA256) {
                "Downloaded model failed its SHA-256 integrity check"
            }
            check(partial.renameTo(modelFile)) { "Could not install the downloaded model" }
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
