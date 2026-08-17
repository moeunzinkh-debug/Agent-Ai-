package com.example.llama

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Kotlin front end for the llama.cpp JNI bridge.
 *
 * Compiled from llama.cpp source targeting Android 7.0 (API 24). All calls are funnelled
 * onto a single thread because the native side holds one global context.
 */
object LlamaBridge {

    /** Thrown when the native engine cannot produce a real response. */
    class LlamaException(message: String) : Exception(message)

    @Volatile
    private var nativeAvailable: Boolean? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val llamaDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

    private external fun nativeInit(): Int
    private external fun nativeLoadModel(path: String, nCtx: Int, nThreads: Int): Int
    private external fun nativeStartCompletion(prompt: String, maxTokens: Int): Int
    private external fun nativeNextToken(): String?
    private external fun nativeStopCompletion()
    private external fun nativeUnloadModel()
    private external fun nativeIsModelLoaded(): Boolean

    /** True when the .so for this device's ABI loaded successfully. */
    fun isAvailable(): Boolean {
        nativeAvailable?.let { return it }
        val ok = try {
            System.loadLibrary("agentai_llama")
            nativeInit() == 0
        } catch (e: UnsatisfiedLinkError) {
            false
        } catch (e: Exception) {
            false
        }
        nativeAvailable = ok
        return ok
    }

    fun isModelLoaded(): Boolean = isAvailable() && nativeIsModelLoaded()

    /**
     * Thread count tuned for phones.
     *
     * Phones are big.LITTLE: availableProcessors() counts the slow efficiency cores too.
     * ggml synchronises all threads at every layer, so the fast cores end up waiting on
     * the slow ones — past ~4 threads throughput usually gets *worse*, not better.
     */
    private fun defaultThreads(): Int =
        Runtime.getRuntime().availableProcessors().let { (it / 2).coerceIn(2, 4) }

    suspend fun loadModel(
        modelFile: File,
        contextLength: Int = 2048,
        threads: Int = defaultThreads()
    ) = withContext(llamaDispatcher) {
        if (!isAvailable()) {
            throw LlamaException("The on-device AI engine is not supported on this device's CPU.")
        }
        if (!modelFile.exists() || !modelFile.canRead()) {
            throw LlamaException("Model file is missing or unreadable.")
        }

        when (val rc = nativeLoadModel(modelFile.absolutePath, contextLength, threads)) {
            0 -> Unit
            -2 -> throw LlamaException("The model file is corrupted or not a valid GGUF.")
            -3 -> throw LlamaException("Not enough memory to load the model on this device.")
            else -> throw LlamaException("Could not load the model (code $rc).")
        }
    }

    /** Streams generated text. Each emission is a UTF-8 safe fragment. */
    fun generate(prompt: String, maxTokens: Int): Flow<String> = flow {
        when (val rc = nativeStartCompletion(prompt, maxTokens)) {
            0 -> Unit
            -4 -> throw LlamaException("This conversation is too long for the model's context window.")
            else -> throw LlamaException("Could not start generation (code $rc).")
        }

        try {
            while (true) {
                val piece = nativeNextToken() ?: break
                if (piece.isNotEmpty()) emit(piece)
            }
        } finally {
            nativeStopCompletion()
        }
    }.flowOn(llamaDispatcher)

    suspend fun unload() = withContext(llamaDispatcher) {
        if (nativeAvailable == true) nativeUnloadModel()
    }
}
