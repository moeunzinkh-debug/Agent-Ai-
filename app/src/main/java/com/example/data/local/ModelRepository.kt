package com.example.data.local

import android.content.Context
import com.example.data.model.LocalModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Downloads and stores the GGUF weights used for on-device inference.
 *
 * The download is resumable (HTTP Range) so a dropped connection on a ~0.8 GB file does not
 * force the user to start over.
 */
class ModelRepository(private val context: Context) {

    sealed interface DownloadProgress {
        data class Downloading(
            val bytesDownloaded: Long,
            val totalBytes: Long
        ) : DownloadProgress {
            val percent: Int
                get() = if (totalBytes <= 0) 0 else ((bytesDownloaded * 100) / totalBytes).toInt()
        }

        data class Completed(val file: File) : DownloadProgress
        data class Failed(val error: String) : DownloadProgress
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun modelsDir(): File =
        File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    fun modelFile(model: LocalModel): File = File(modelsDir(), model.fileName)

    private fun partialFile(model: LocalModel): File =
        File(modelsDir(), "${model.fileName}.part")

    /** True when the full, correctly sized weights are already on disk. */
    fun isDownloaded(model: LocalModel): Boolean {
        val file = modelFile(model)
        return file.exists() && file.length() == model.sizeBytes
    }

    fun downloadedBytes(model: LocalModel): Long {
        val complete = modelFile(model)
        if (complete.exists()) return complete.length()
        val partial = partialFile(model)
        return if (partial.exists()) partial.length() else 0L
    }

    fun deleteModel(model: LocalModel): Boolean {
        val deletedFull = modelFile(model).let { if (it.exists()) it.delete() else true }
        val deletedPart = partialFile(model).let { if (it.exists()) it.delete() else true }
        return deletedFull && deletedPart
    }

    /** Free space available for the model, in bytes. */
    fun availableSpaceBytes(): Long = modelsDir().usableSpace

    /**
     * Streams the weights to disk, emitting progress. Safe to call again after a failure:
     * it resumes from the bytes already fetched.
     */
    fun download(model: LocalModel): Flow<DownloadProgress> = flow {
        val target = modelFile(model)
        if (target.exists() && target.length() == model.sizeBytes) {
            emit(DownloadProgress.Completed(target))
            return@flow
        }
        // A truncated/corrupt full file is worthless — start it over.
        if (target.exists() && target.length() != model.sizeBytes) target.delete()

        val partial = partialFile(model)
        var alreadyHave = if (partial.exists()) partial.length() else 0L
        if (alreadyHave > model.sizeBytes) {
            partial.delete()
            alreadyHave = 0L
        }

        if (availableSpaceBytes() < (model.sizeBytes - alreadyHave) + SAFETY_MARGIN_BYTES) {
            emit(DownloadProgress.Failed("Not enough free storage for ${model.sizeLabel}."))
            return@flow
        }

        emit(DownloadProgress.Downloading(alreadyHave, model.sizeBytes))

        val requestBuilder = Request.Builder().url(model.downloadUrl)
        if (alreadyHave > 0) requestBuilder.header("Range", "bytes=$alreadyHave-")

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} while downloading the model")
                }
                // If the server ignored our Range header we must rewrite from zero.
                val append = alreadyHave > 0 && response.code == 206
                if (!append) alreadyHave = 0L

                val body = response.body ?: throw IOException("Empty response body")

                body.byteStream().use { input ->
                    java.io.FileOutputStream(partial, append).use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER)
                        var written = alreadyHave
                        var lastEmitted = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            written += read
                            // Throttle UI updates to roughly every 2 MB.
                            if (written - lastEmitted >= PROGRESS_STEP) {
                                lastEmitted = written
                                emit(DownloadProgress.Downloading(written, model.sizeBytes))
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (partial.length() != model.sizeBytes) {
                partial.delete()
                emit(DownloadProgress.Failed("Download incomplete or corrupted. Please try again."))
                return@flow
            }

            if (!partial.renameTo(target)) {
                emit(DownloadProgress.Failed("Could not save the model file."))
                return@flow
            }

            emit(DownloadProgress.Completed(target))
        } catch (e: IOException) {
            emit(DownloadProgress.Failed(e.message ?: "Network error while downloading the model."))
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val DOWNLOAD_BUFFER = 64 * 1024
        const val PROGRESS_STEP = 2L * 1024 * 1024
        const val SAFETY_MARGIN_BYTES = 128L * 1024 * 1024
    }
}
