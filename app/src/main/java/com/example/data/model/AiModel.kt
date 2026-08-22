package com.example.data.model

/** Models exposed by the app. Local model weights are downloaded on first use. */
data class AiModel(
    val id: String,
    val displayName: String,
    val isLocal: Boolean = false
)

object AiModels {
    // Keep the user-facing id spelling for backwards-compatible settings. The upstream GGUF
    // convention uses Q8_0 (underscore), represented by [SMOLLM_DOWNLOAD_FILENAME].
    const val SMOLLM_135M_UNCENSORED = "smollm_135m_uncensored.Q8.0.gguf"
    const val SMOLLM_DOWNLOAD_FILENAME = "smollm_135m_uncensored.Q8_0.gguf"
    const val SMOLLM_REPOSITORY = "arzaan789/smollm-135m-uncensored"
    const val SMOLLM_SIZE_BYTES = 144_810_752L
    const val SMOLLM_SHA256 = "d59e2b67f5da3f4a73d4afd20331f11381785063d1f62b1c333a790ad80454f6"

    val all = listOf(
        AiModel("gemma-4-flash", "Gemma 4 Flash"),
        AiModel("gemma-4-pro", "Gemma 4 Pro"),
        AiModel("gemini-3.5-flash", "Gemini 3.5"),
        AiModel(SMOLLM_135M_UNCENSORED, "SmolLM 135M Q8 (Local)", isLocal = true)
    )

    fun isSmolLm(id: String): Boolean =
        id == SMOLLM_135M_UNCENSORED || id == SMOLLM_DOWNLOAD_FILENAME

    fun getById(id: String): AiModel =
        if (isSmolLm(id)) all.last() else all.firstOrNull { it.id == id } ?: all.first()
}
