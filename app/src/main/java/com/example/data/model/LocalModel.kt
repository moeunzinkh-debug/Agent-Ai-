package com.example.data.model

/**
 * A real GGUF model that runs fully on-device through llama.cpp.
 *
 * Nothing here is a canned/preset response: the app downloads these weights once and every
 * answer is produced by real token-by-token inference on the phone's CPU.
 */
data class LocalModel(
    /** Stable id persisted in settings. */
    val id: String,
    /** Human readable name. */
    val displayName: String,
    /** Direct download URL of the GGUF weights. */
    val downloadUrl: String,
    /** File name used on disk. */
    val fileName: String,
    /** Exact size in bytes of the GGUF file (used for progress + integrity check). */
    val sizeBytes: Long,
    /** Short size label shown in the UI, e.g. "0.8 GB". */
    val sizeLabel: String,
    /** Description in English. */
    val description: String,
    /** Description in Khmer. */
    val descriptionKm: String,
    /** True when the model has no refusal/safety alignment layer. */
    val isUncensored: Boolean = false,
    /** Context window to request from llama.cpp. */
    val contextLength: Int = 4096
) {
    val sizeMb: Long get() = sizeBytes / (1024 * 1024)
}

object LocalModels {

    /**
     * Llama 3.2 1B Instruct (abliterated) — the smallest genuinely capable uncensored
     * open-source instruct model. Q3_K_M quantisation is ~0.8 GB, which fits comfortably
     * on mid-range phones while still running at a usable speed on CPU.
     */
    val LLAMA_3_2_1B_INSTRUCT = LocalModel(
        id = "llama-3.2-1b-instruct",
        displayName = "Llama 3.2 1B Instruct",
        downloadUrl = "https://huggingface.co/mradermacher/Llama-3.2-1B-Instruct-abliterated-GGUF/" +
                "resolve/main/Llama-3.2-1B-Instruct-abliterated.Q3_K_M.gguf?download=true",
        fileName = "llama-3.2-1b-instruct-q3_k_m.gguf",
        sizeBytes = 803_709_472L,
        sizeLabel = "0.8 GB",
        description = "Smallest uncensored open-source model. Runs fully offline on your device.",
        descriptionKm = "ម៉ូដែលបើកចំហ uncensored តូចបំផុត។ ដំណើរការលើទូរស័ព្ទរបស់អ្នកទាំងស្រុង។",
        isUncensored = true,
        contextLength = 4096
    )

    val ALL: List<LocalModel> = listOf(LLAMA_3_2_1B_INSTRUCT)

    val DEFAULT: LocalModel = LLAMA_3_2_1B_INSTRUCT

    fun getById(id: String?): LocalModel = ALL.find { it.id == id } ?: DEFAULT
}
