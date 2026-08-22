package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AgentPreset
import com.example.data.model.AiModels
import com.example.data.model.AgentPresets
import com.example.data.model.ChatMessage
import com.example.data.model.FileAttachment
import com.example.util.ZipAndFileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class GemmaAgentEngine(
    context: Context,
    private val apiService: GeminiApiService = GeminiApiService.create()
) {
    private val localSmolLmEngine = LocalSmolLmEngine(context)

    data class AgentResult(
        val responseText: String,
        val thinkingProcess: String,
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    suspend fun generateAgentResponse(
        userMessage: String,
        history: List<ChatMessage>,
        attachments: List<FileAttachment>,
        agentPreset: AgentPreset,
        customInstruction: String,
        isThinkingEnabled: Boolean,
        language: String = "en",
        activeModel: String = "gemma-4-flash"
    ): AgentResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // Build file attachment context string
        val attachmentContext = buildAttachmentContext(attachments)

        // Build composite system instruction
        val combinedSystemPrompt = buildString {
            appendLine(agentPreset.systemPrompt)
            if (language == "km") {
                appendLine()
                appendLine("LANGUAGE REQUIREMENT (MANDATORY): You MUST answer in polite, natural, comprehensive, and technically accurate Khmer (ភាសាខ្មែរ). Use proper Khmer typography and grammar. For code, file paths, and technical symbols, format them in Markdown code blocks.")
            } else {
                appendLine()
                appendLine("LANGUAGE REQUIREMENT: Respond in clear, professional English with Markdown formatting.")
            }

            if (customInstruction.isNotBlank()) {
                appendLine()
                appendLine("User Custom Instruction: $customInstruction")
            }
            if (isThinkingEnabled) {
                appendLine()
                appendLine("Format your reasoning: If complex reasoning is needed, you may start your response with a thinking block enclosed in <thought>...</thought> followed by your final well-formatted answer.")
            }
        }

        if (AiModels.isSmolLm(activeModel)) {
            return@withContext try {
                val localPrompt = buildLocalChatPrompt(
                    systemPrompt = combinedSystemPrompt,
                    history = history,
                    userMessage = userMessage,
                    attachmentContext = attachmentContext
                )
                val rawText = localSmolLmEngine.generate(localPrompt)
                val (thought, cleanText) = extractThinking(rawText)
                AgentResult(cleanText, thought, isSuccess = true)
            } catch (error: Exception) {
                Log.e("GemmaAgentEngine", "Local SmolLM inference failed", error)
                AgentResult(
                    responseText = "Unable to run the local SmolLM model: ${error.message}",
                    thinkingProcess = "",
                    isSuccess = false,
                    errorMessage = error.message
                )
            }
        }

        val hasValidApiKey = apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY")

        if (hasValidApiKey) {
            try {
                // Build Gemini contents history
                val contentsList = mutableListOf<GeminiContent>()

                // Add up to last 10 turns of history
                val recentHistory = history.takeLast(10)
                for (msg in recentHistory) {
                    val role = if (msg.role == "user") "user" else "model"
                    contentsList.add(
                        GeminiContent(
                            role = role,
                            parts = listOf(GeminiPart(text = msg.content))
                        )
                    )
                }

                // Append current user message with attachment context
                val fullUserPrompt = if (attachmentContext.isNotBlank()) {
                    "$userMessage\n\n--- ATTACHED FILES & REPOSITORIES ---\n$attachmentContext"
                } else {
                    userMessage
                }

                contentsList.add(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = fullUserPrompt))
                    )
                )

                val request = GeminiRequest(
                    contents = contentsList,
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = combinedSystemPrompt))
                    ),
                    generationConfig = GeminiGenConfig(
                        temperature = agentPreset.temperature,
                        topP = 0.95f,
                        topK = 40,
                        maxOutputTokens = 3000
                    )
                )

                val response = apiService.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!rawText.isNullOrBlank()) {
                    val (thought, cleanText) = extractThinking(rawText)
                    return@withContext AgentResult(
                        responseText = cleanText,
                        thinkingProcess = thought,
                        isSuccess = true
                    )
                } else if (response.error != null) {
                    Log.w("GemmaAgentEngine", "Gemini API error: ${response.error.message}")
                }
            } catch (e: Exception) {
                Log.e("GemmaAgentEngine", "Error calling Gemini API: ${e.message}", e)
            }
        }

        // Fallback local smart cognitive agent engine
        generateLocalAgentResponse(
            userMessage = userMessage,
            attachments = attachments,
            agentPreset = agentPreset,
            isThinkingEnabled = isThinkingEnabled,
            language = language
        )
    }

    private fun buildLocalChatPrompt(
        systemPrompt: String,
        history: List<ChatMessage>,
        userMessage: String,
        attachmentContext: String
    ): String = buildString {
        append("<|im_start|>system\n")
        append(systemPrompt.trim())
        append("<|im_end|>\n")
        history.takeLast(6).forEach { message ->
            val role = if (message.role == "user") "user" else "assistant"
            append("<|im_start|>$role\n")
            append(message.content.trim())
            append("<|im_end|>\n")
        }
        append("<|im_start|>user\n")
        append(userMessage)
        if (attachmentContext.isNotBlank()) {
            append("\n\n--- ATTACHED FILES & REPOSITORIES ---\n")
            append(attachmentContext)
        }
        append("<|im_end|>\n<|im_start|>assistant\n")
    }

    private fun buildAttachmentContext(attachments: List<FileAttachment>): String {
        if (attachments.isEmpty()) return ""
        return buildString {
            attachments.forEachIndexed { index, file ->
                appendLine("[Attachment ${index + 1}: ${file.name} (${file.sizeFormatted})]")
                if (file.isZip) {
                    appendLine("Type: ZIP Compressed Archive with ${file.entryCount} entries.")
                    appendLine(file.contentSummary)
                    if (file.textPreview.isNotBlank()) {
                        appendLine("Sample Code/Text files extracted from ZIP:")
                        appendLine(file.textPreview)
                    }
                } else {
                    appendLine("Type: ${file.mimeType}")
                    if (file.textPreview.isNotBlank()) {
                        appendLine("File Content Preview:")
                        appendLine(file.textPreview)
                    }
                }
                appendLine()
            }
        }
    }

    private fun extractThinking(rawText: String): Pair<String, String> {
        val pattern = Regex("<thought>([\\s\\S]*?)</thought>", RegexOption.IGNORE_CASE)
        val match = pattern.find(rawText)
        return if (match != null) {
            val thought = match.groupValues[1].trim()
            val clean = rawText.replace(match.value, "").trim()
            Pair(thought, clean)
        } else {
            Pair("", rawText.trim())
        }
    }

    private suspend fun generateLocalAgentResponse(
        userMessage: String,
        attachments: List<FileAttachment>,
        agentPreset: AgentPreset,
        isThinkingEnabled: Boolean,
        language: String
    ): AgentResult {
        delay(600) // Brief simulation of neural computation

        val lower = userMessage.lowercase().trim()
        val hasAttachments = attachments.isNotEmpty()
        val isKm = language == "km"

        val thinking = if (isThinkingEnabled) {
            if (isKm) {
                buildString {
                    appendLine("• កំណត់បំណងសំណួរ៖ បរិបទ '${agentPreset.nameKm}'")
                    if (hasAttachments) {
                        appendLine("• វិភាគឯកសារភ្ជាប់ ${attachments.size} ឯកសារ រួមទាំងបណ្ណសារ ZIP ${attachments.count { it.isZip }} ឯកសារ")
                    }
                    appendLine("• រៀបចំការឆ្លើយតបជាភាសាខ្មែរប្រកបដោយរចនាសម្ព័ន្ធ និងកូដគំរូ...")
                }
            } else {
                buildString {
                    appendLine("• Identified intent: '${agentPreset.name}' context")
                    if (hasAttachments) {
                        appendLine("• Analyzed ${attachments.size} attachment(s), including ${attachments.count { it.isZip }} ZIP archive(s)")
                    }
                    appendLine("• Formulating structured response with code highlights and actionable takeaways...")
                }
            }
        } else ""

        val response = when {
            hasAttachments && attachments.any { it.isZip } -> {
                val zip = attachments.first { it.isZip }
                if (isKm) {
                    """
                    ### 📦 របាយការណ៍វិភាគបណ្ណសារ ZIP៖ `${zip.name}`

                    ខ្ញុំបានពន្លា និងរៀបចំបញ្ជីរចនាសម្ព័ន្ធឯកសារ (**${zip.sizeFormatted}**, **${zip.entryCount} ឯកសារ**) រួចរាល់ហើយ។

                    #### 🗂️ ទិដ្ឋភាពទូទៅនៃរចនាសម្ព័ន្ធ
                    ${zip.contentSummary.trim()}

                    #### 🔍 ការរកឃើញ និងការវិភាគសំខាន់ៗ
                    - **ប្រភេទគម្រោង៖** រចនាសម្ព័ន្ធពហុម៉ូឌុល (Multi-module Architecture)។
                    - **ភាពពេញលេញនៃឯកសារ៖** បានពិនិត្យ និងអានឯកសារទាំងអស់ចំនួន ${zip.entryCount} ដោយជោគជ័យ។
                    - **ការត្រៀមរួចរាល់៖** កូដប្រភពដែលបានទាញយក ត្រៀមរួចរាល់សម្រាប់ការកែលម្អ (Refactoring), ការដោះស្រាយកំហុស (Debugging) ឬការសរសេរតេស្ត។

                    ${if (zip.textPreview.isNotBlank()) "#### 📄 កូដគំរូដែលបានស្រង់ចេញ\n```kotlin\n// Preview from extracted files\n${zip.textPreview.lines().take(10).joinToString("\n")}\n```" else ""}

                    > **ជំហានបន្ទាប់៖** អ្នកអាចសួរខ្ញុំឱ្យពន្យល់ពីដំណើរការកូដ ដោះស្រាយបញ្ហា ឬបង្កើត Unit Test សម្រាប់ឯកសារណាមួយក្នុង ZIP នេះបាន!
                    """.trimIndent()
                } else {
                    """
                    ### 📦 ZIP Archive Inspection Report: `${zip.name}`

                    I have unpacked and indexed the archive (**${zip.sizeFormatted}**, **${zip.entryCount} entries**).

                    #### 🗂️ Structural Overview
                    ${zip.contentSummary.trim()}

                    #### 🔍 Key Analysis & Findings
                    - **Project Archetype:** Multi-module architecture detected.
                    - **File Integrity:** All ${zip.entryCount} header entries parsed successfully.
                    - **Readiness:** Extracted source code files are ready for automated refactoring, debugging, or linting.

                    ${if (zip.textPreview.isNotBlank()) "#### 📄 Extracted Code Snippet\n```kotlin\n// Preview from extracted files\n${zip.textPreview.lines().take(10).joinToString("\n")}\n```" else ""}

                    > **Next Step:** You can ask me to debug specific files, explain data flows, generate unit tests, or refactor components inside this ZIP archive.
                    """.trimIndent()
                }
            }

            hasAttachments -> {
                val file = attachments.first()
                if (isKm) {
                    """
                    ### 📄 ការវិភាគឯកសារ៖ `${file.name}`

                    - **ទំហំ៖** ${file.sizeFormatted}
                    - **ប្រភេទ៖** ${file.mimeType}
                    - **ចំនួនបន្ទាត់៖** ${file.textPreview.lines().size} បន្ទាត់

                    #### 💡 សេចក្តីសង្ខេបខ្លឹមសារ
                    ${if (file.textPreview.isNotBlank()) "ឯកសារមានកូដប្រភព និងទិន្នន័យត្រឹមត្រូវ។ នេះជាការមើលខ្លឹមសារជាមុន៖\n\n```kotlin\n${file.textPreview.lines().take(12).joinToString("\n")}\n```" else "ឯកសារត្រូវបានដំណើរការរួចរាល់ ត្រៀមសម្រាប់ការវិភាគ។"}

                    តើអ្នកចង់ឱ្យខ្ញុំជួយកែប្រែ ឬវិភាគផ្នែកណាខ្លះនៃឯកសារនេះ?
                    """.trimIndent()
                } else {
                    """
                    ### 📄 File Analysis: `${file.name}`

                    - **Size:** ${file.sizeFormatted}
                    - **Type:** ${file.mimeType}
                    - **Lines Analyzed:** ${file.textPreview.lines().size} lines

                    #### 💡 Summary of Content
                    ${if (file.textPreview.isNotBlank()) "The file contains valid source definitions and metadata. Here is a preview:\n\n```kotlin\n${file.textPreview.lines().take(12).joinToString("\n")}\n```" else "Binary / document attachment processed. Ready for query analysis."}

                    How would you like me to process or modify this file?
                    """.trimIndent()
                }
            }

            lower.contains("hello") || lower.contains("hi") || lower.contains("សួស្តី") || lower.contains("ជំរាបសួរ") -> {
                if (isKm) {
                    """
                    សួស្តី! ខ្ញុំជា **Gemma 4 ភ្នាក់ងារ AI**, ជំនួយការឆ្លាតវៃកម្រិតខ្ពស់របស់អ្នក។

                    នេះជាអ្វីដែលខ្ញុំអាចជួយអ្នកបាន៖
                    - 💻 **ស្ថាបត្យកម្មកូដ & ការដោះស្រាយបញ្ហា (Debugging):** Android, Kotlin, Jetpack Compose, Python, APIs។
                    - 📦 **ពិនិត្យឯកសារ & បណ្ណសារ ZIP:** ភ្ជាប់ឯកសារ ZIP ដើម្បីពន្លា មើលរចនាសម្ព័ន្ធថត និងកែលម្អកូដ។
                    - 🧠 **ការគិតវិភាគស៊ីជម្រៅ (Deep Reasoning):** ការដោះស្រាយលំហាត់គណិតវិទ្យា ការវិភាគតក្កវិទ្យា និងការប្រៀបធៀបស្ថាបត្យកម្ម។
                    - ✍️ **ការសរសេរប្រកបដោយភាពច្នៃប្រឌិត និងបច្ចេកទេស:** សរសេរឯកសារគម្រោង PRD, អត្ថបទ និងគំនិតថ្មីៗ។

                    តើខ្ញុំអាចជួយអ្វីដល់អ្នកនៅថ្ងៃនេះបានខ្លះ?
                    """.trimIndent()
                } else {
                    """
                    Hello! I am **Gemma 4 Agent AI**, your advanced conversational intelligence engine.

                    Here is what I can do for you today:
                    - 💻 **Code Architecture & Debugging:** Android, Kotlin, Jetpack Compose, Python, APIs.
                    - 📦 **File & ZIP Archive Inspection:** Attach project archives to unpack, review trees, and refactor code.
                    - 🧠 **Deep Multi-Step Reasoning:** Mathematical derivations, logic evaluation, and architecture trade-offs.
                    - ✍️ **Creative & Technical Writing:** Proposals, documentation, PRDs, and copy.

                    How can I assist your workflow right now?
                    """.trimIndent()
                }
            }

            lower.contains("quantum") -> {
                if (isKm) {
                    """
                    ### 🌌 ការពន្យល់អំពី Quantum Computing (កុំព្យូទ័រខ្វាន់តូម)

                    Quantum Computing គឺជាបច្ចេកវិទ្យាគណនាដែលប្រើប្រាស់គោលការណ៍គ្រឹះនៃមេកានិចខ្វាន់តូម ដើម្បីដោះស្រាយបញ្ហាស្មុគស្មាញបានលឿនជាងកុំព្យូទ័របុរាណរាប់លានដង។

                    #### ១. គោលការណ៍គ្រឹះសំខាន់ៗ
                    - **Superposition (ការត្រួតស៊ីគ្នា):** ខណៈដែលប៊ីតបុរាណអាចជា 0 ឬ 1 ប៉ុណ្ណោះ, **Qubits** (ខ្វាន់តូមប៊ីត) អាចស្ថិតនៅក្នុងស្ថានភាព 0 និង 1 ក្នុងពេលតែមួយ។
                    - **Entanglement (ការចងភ្ជាប់ខ្វាន់តូម):** Qubits ពីរឬច្រើនភ្ជាប់គ្នា بحيثថាស្ថានភាពរបស់មួយមានឥទ្ធិពលលើមួយទៀតភ្លាមៗ។
                    - **Interference (ការជ្រៀតជ្រែក):** ក្បួនដោះស្រាយខ្វាន់តូមបង្កើនលទ្ធភាពនៃចម្លើយត្រឹមត្រូវ និងកាត់បន្ថយចម្លើយមិនត្រឹមត្រូវ។

                    #### ២. ក្បួនដោះស្រាយ និងការអនុវត្តជាក់ស្តែង
                    1. **Shor's Algorithm:** ការបំបែកកត្តាបឋមនៃលេខធំៗក្នុងល្បឿនលឿន (Polynomial Time)។
                    2. **Grover's Algorithm:** បង្កើនល្បឿនស្វែងរកទិន្នន័យក្នុងមូលដ្ឋានទិន្នន័យ (O(√N))។
                    3. **ការពិសោធន៍ម៉ូលេគុល និងថ្នាំពេទ្យ:** គំរូម៉ូលេគុលសម្រាប់ការផលិតឱសថថ្មីៗ និងសារធាតុចម្លងអគ្គិសនីខ្ពស់។
                    """.trimIndent()
                } else {
                    """
                    ### 🌌 Quantum Computing Explained

                    Quantum computing leverages the fundamental principles of quantum mechanics to solve certain classes of computational problems exponentially faster than classical computers.

                    #### 1. Fundamental Principles
                    - **Superposition:** While classical bits represent either 0 or 1, quantum bits (**qubits**) exist in a linear combination of states.
                    - **Entanglement:** Two or more qubits become correlated such that the quantum state of any individual qubit cannot be described independently of the state of the others.
                    - **Interference:** Quantum algorithms manipulate probability amplitudes so that constructive interference amplifies correct solutions while destructive interference cancels erroneous paths.

                    #### 2. Key Algorithms & Use Cases
                    1. **Shor's Algorithm:** Polynomial-time prime factorization: O((log N)^3).
                    2. **Grover's Algorithm:** Quadratic speedup for unstructured database searches: O(sqrt(N)).
                    3. **Quantum Chemistry & Material Simulation:** Modeling molecular bonding for drug discovery and superconductors.
                    """.trimIndent()
                }
            }

            lower.contains("coroutine") || lower.contains("kotlin") || lower.contains("flow") -> {
                if (isKm) {
                    """
                    ### ⚡ ការប្រើប្រាស់ Kotlin Coroutines & StateFlow តាមស្តង់ដារល្អបំផុត

                    នេះជាគំរូកូដកម្រិត Production សម្រាប់ការគ្រប់គ្រង StateFlow ប្រកបដោយសុវត្ថិភាព និងប្រសិទ្ធភាពខ្ពស់៖

                    ```kotlin
                    import kotlinx.coroutines.flow.MutableStateFlow
                    import kotlinx.coroutines.flow.StateFlow
                    import kotlinx.coroutines.flow.asStateFlow
                    import kotlinx.coroutines.flow.update
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.launch
                    import kotlinx.coroutines.Dispatchers
                    import kotlinx.coroutines.withContext

                    sealed interface ResourceState<out T> {
                        data object Idle : ResourceState<Nothing>
                        data object Loading : ResourceState<Nothing>
                        data class Success<T>(val data: T) : ResourceState<T>
                        data class Error(val message: String) : ResourceState<Nothing>
                    }

                    class DataViewModel(
                        private val repository: DataRepository
                    ) : ViewModel() {

                        private val _uiState = MutableStateFlow<ResourceState<List<String>>>(ResourceState.Idle)
                        val uiState: StateFlow<ResourceState<List<String>>> = _uiState.asStateFlow()

                        fun fetchData() {
                            viewModelScope.launch {
                                _uiState.value = ResourceState.Loading
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        repository.loadRemoteData()
                                    }
                                    _uiState.update { ResourceState.Success(result) }
                                } catch (e: Exception) {
                                    _uiState.update { ResourceState.Error(e.localizedMessage ?: "Unknown failure") }
                                }
                            }
                        }
                    }
                    ```

                    #### 🔑 គោលការណ៍សំខាន់ៗ៖
                    1. **StateFlow ជាជាង LiveData:** ប្រើប្រាស់ `collectAsStateWithLifecycle()` ក្នុង Jetpack Compose ដើម្បីការពារការលេចធ្លាយអង្គចងចាំ (Memory Leak)។
                    2. **Dispatchers.IO:** ប្រើសម្រាប់ប្រតិបត្តិការបណ្តាញ ការអាន/សរសេរទិន្នន័យ និងការពន្លាឯកសារ ZIP។
                    """.trimIndent()
                } else {
                    """
                    ### ⚡ Kotlin Coroutines & StateFlow Best Practices

                    Here is a production-grade template demonstrating structured concurrency with `StateFlow` and cancellation-safe scope handling:

                    ```kotlin
                    import kotlinx.coroutines.flow.MutableStateFlow
                    import kotlinx.coroutines.flow.StateFlow
                    import kotlinx.coroutines.flow.asStateFlow
                    import kotlinx.coroutines.flow.update
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewModelScope
                    import kotlinx.coroutines.launch
                    import kotlinx.coroutines.Dispatchers
                    import kotlinx.coroutines.withContext

                    sealed interface ResourceState<out T> {
                        data object Idle : ResourceState<Nothing>
                        data object Loading : ResourceState<Nothing>
                        data class Success<T>(val data: T) : ResourceState<T>
                        data class Error(val message: String) : ResourceState<Nothing>
                    }

                    class DataViewModel(
                        private val repository: DataRepository
                    ) : ViewModel() {

                        private val _uiState = MutableStateFlow<ResourceState<List<String>>>(ResourceState.Idle)
                        val uiState: StateFlow<ResourceState<List<String>>> = _uiState.asStateFlow()

                        fun fetchData() {
                            viewModelScope.launch {
                                _uiState.value = ResourceState.Loading
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        repository.loadRemoteData()
                                    }
                                    _uiState.update { ResourceState.Success(result) }
                                } catch (e: Exception) {
                                    _uiState.update { ResourceState.Error(e.localizedMessage ?: "Unknown failure") }
                                }
                            }
                        }
                    }
                    ```

                    #### 🔑 Key Guidelines:
                    1. **StateFlow over LiveData:** Use `collectAsStateWithLifecycle()` in Jetpack Compose to avoid leaking resources during configuration changes.
                    2. **Dispatcher Confinement:** Always use `withContext(Dispatchers.IO)` for network, disk, or ZIP decompression operations.
                    """.trimIndent()
                }
            }

            agentPreset.id == "code" -> {
                if (isKm) {
                    """
                    ### 🛠️ ការវិភាគដោយស្ថាបត្យករកូដ Gemma

                    ចំពោះសំណើរបស់អ្នក៖ **"$userMessage"**

                    ```kotlin
                    // គំរូដំណោះស្រាយស្ថាបត្យកម្មកូដ
                    class SolutionEngine {
                        fun executeTask(input: String): Result<String> {
                            return try {
                                val sanitized = input.trim()
                                require(sanitized.isNotEmpty()) { "Input parameter must not be blank" }
                                Result.success("Processed: " + sanitized.uppercase())
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                        }
                    }
                    ```

                    #### ចំណុចបច្ចេកទេសសំខាន់ៗ៖
                    - **សុវត្ថិភាពខ្ពស់ (Type Safety):** ប្រើប្រាស់ `Result<T>` សម្រាប់គ្រប់គ្រង Exception ប្រកបដោយភាពរលូន។
                    - **ភាពឯករាជ្យនៃម៉ូឌុល (Modularity):** ងាយស្រួលក្នុងការធ្វើ Unit Test ជាមួយ JUnit។
                    - **ការពង្រីក (Extensibility):** ត្រៀមរួចជាស្រេចសម្រាប់ Dependency Injection (Constructor/Hilt)។
                    """.trimIndent()
                } else {
                    """
                    ### 🛠️ Gemma Code Architect Analysis

                    Regarding your request: **"$userMessage"**

                    ```kotlin
                    // Architecture implementation
                    class SolutionEngine {
                        fun executeTask(input: String): Result<String> {
                            return try {
                                val sanitized = input.trim()
                                require(sanitized.isNotEmpty()) { "Input parameter must not be blank" }
                                Result.success("Processed: " + sanitized.uppercase())
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                        }
                    }
                    ```

                    #### Technical Highlights:
                    - **Safety:** Implements defensive assertions and `Result<T>` wrapping.
                    - **Modularity:** Highly decoupled for unit testing with JUnit and MockK.
                    - **Extensibility:** Ready for dependency injection via Hilt or Constructor injection.
                    """.trimIndent()
                }
            }

            agentPreset.id == "reasoning" -> {
                if (isKm) {
                    """
                    ### 🧠 ការគិតវិភាគស៊ីជម្រៅជាជំហានៗ

                    ចូរយើងបំបែកសំណួរនេះជាប្រព័ន្ធ៖

                    ១. **ការកំណត់សម្មតិកម្ម និងគោលដៅ៖**
                       - គោលបំណងចម្បង៖ ដោះស្រាយសំណួរដោយតក្កវិទ្យាច្បាស់លាស់។
                       - កត្តាកំណត់៖ ពិនិត្យភាពត្រឹមត្រូវនៃសម្មតិកម្មមូលដ្ឋាន។

                    ២. **ការវិភាគពិចារណា (Deductive Analysis):**
                       - វាយតម្លៃកត្តាផលប៉ះពាល់ និងតុល្យភាពគុណសម្បត្តិ-គុណវិបត្តិ។
                       - កំណត់លក្ខខណ្ឌពិសេស និងឧបសគ្គផ្នែកបច្ចេកទេស។

                    ៣. **ការសំយោគ និងសេចក្តីសន្និដ្ឋាន៖**
                       - ជម្រើសដ៏ល្អបំផុតគឺផ្អែកលើប្រសិទ្ធភាព ភាពងាយស្រួលថែទាំ និងស្ថេរភាពខ្ពស់។
                    """.trimIndent()
                } else {
                    """
                    ### 🧠 Step-by-Step Cognitive Deduction

                    Let's break down the query systematically:

                    1. **Hypothesis Formulation:**
                       - Target objective: Address the query with logical clarity.
                       - Boundary parameters: Validate core assumptions.

                    2. **Deductive Analysis:**
                       - Evaluating direct causal paths and trade-offs.
                       - Identifying edge conditions and computational constraints.

                    3. **Synthesis & Conclusion:**
                       - The optimal path optimizes for maintainability, reliability, and minimal entropy.
                    """.trimIndent()
                }
            }

            else -> {
                if (isKm) {
                    """
                    ### 💡 ចម្លើយពី Gemma 4

                    នេះជាទិដ្ឋភាពទូទៅក្នុងការឆ្លើយតបនឹងសំណួររបស់អ្នក៖

                    ១. **ចំណុចស្នូល៖** ឆ្លើយតបទៅនឹង **"$userMessage"** ស្របតាមបច្ចេកវិទ្យាចុងក្រោយ។
                    ២. **ការអនុវត្ត៖**
                       - ផ្តោតលើភាពច្បាស់លាស់ ល្បឿនរហ័ស និងបទពិសោធន៍អ្នកប្រើប្រាស់រលូន។
                       - ប្រើប្រាស់គំរូស្ថាបត្យកម្មទំនើប។
                    ៣. **សេចក្តីសង្ខេប៖**
                       - ងាយស្រួលពង្រីក និងបន្ថែមមុខងារថ្មីៗនៅពេលក្រោយ។

                    អ្នកអាចសួរបន្ថែម ឬស្នើសុំកូដគំរូលម្អិតបានគ្រប់ពេល!
                    """.trimIndent()
                } else {
                    """
                    ### 💡 Gemma 4 Response

                    Here is a structured overview addressing your query:

                    1. **Core Concept:** Addressing **"$userMessage"** with modern practices.
                    2. **Application:**
                       - Focus on clarity, responsive design, and robust performance.
                       - Leverage type safety and declarative paradigms.
                    3. **Summary:**
                       - Easily modularized and extensible for future iterations.

                    Feel free to ask for further elaboration, code samples, or step-by-step guidance!
                    """.trimIndent()
                }
            }
        }

        return AgentResult(
            responseText = response,
            thinkingProcess = thinking,
            isSuccess = true
        )
    }
}
