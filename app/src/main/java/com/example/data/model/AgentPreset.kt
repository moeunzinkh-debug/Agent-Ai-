package com.example.data.model

data class AgentPreset(
    val id: String,
    val name: String,
    val nameKm: String,
    val icon: String,
    val description: String,
    val descriptionKm: String,
    val systemPrompt: String,
    val temperature: Float = 0.7f
) {
    fun getDisplayName(languageCode: String): String {
        return if (languageCode == "km") nameKm else name
    }

    fun getDisplayDescription(languageCode: String): String {
        return if (languageCode == "km") descriptionKm else description
    }
}

object AgentPresets {
    val ALL = listOf(
        AgentPreset(
            id = "general",
            name = "General Agent",
            nameKm = "ភ្នាក់ងារទូទៅ",
            icon = "Sparkles",
            description = "All-around intelligent assistant for general questions and tasks",
            descriptionKm = "ជំនួយការឆ្លាតវៃគ្រប់ជ្រុងជ្រោយ សម្រាប់សំណួរទូទៅ និងកិច្ចការប្រចាំថ្ងៃ",
            systemPrompt = "You are Agent AI, a helpful assistant running entirely on the user's own device. Provide clear, accurate, structured, and helpful responses. Use Markdown for formatting and code blocks for programming snippets."
        ),
        AgentPreset(
            id = "code",
            name = "Code Architect",
            nameKm = "ស្ថាបត្យករកូដ",
            icon = "Terminal",
            description = "Specialized in Android, Kotlin, Python, algorithms, architecture & debugging",
            descriptionKm = "ឯកទេសខាង Android, Kotlin, Python, ក្បួនដោះស្រាយ, ស្ថាបត្យកម្មប្រព័ន្ធ និងការដោះស្រាយបញ្ហាកូដ",
            systemPrompt = "You are Agent AI in Code Architect mode, an expert senior software engineer specializing in Android, Kotlin, Jetpack Compose, Python, modern cloud architecture, and debugging. Provide production-ready, clean, well-commented code, explain design patterns, and proactively analyze attached source files or ZIP project trees."
        ),
        AgentPreset(
            id = "reasoning",
            name = "Deep Reasoning",
            nameKm = "ការគិតស៊ីជម្រៅ",
            icon = "Psychology",
            description = "Step-by-step cognitive deduction, math, logic, and critical analysis",
            descriptionKm = "ការគិតវិភាគជាជំហានៗតាមបែបវិទ្យាសាស្ត្រ គណិតវិទ្យា តក្កវិទ្យា និងការវិភាគស៊ីជម្រៅ",
            systemPrompt = "You are Agent AI in Deep Reasoning mode. Break down complex problems step-by-step with clear logic, mathematical precision, chain-of-thought deductions, and thorough evaluation of edge cases."
        ),
        AgentPreset(
            id = "analyst",
            name = "Data & File Analyst",
            nameKm = "អ្នកវិភាគទិន្នន័យ & ឯកសារ",
            icon = "Analytics",
            description = "Inspects ZIP archives, JSON/CSV data, file structures, and project metrics",
            descriptionKm = "ពិនិត្យឯកសារ ZIP, ទិន្នន័យ JSON/CSV, រចនាសម្ព័ន្ធឯកសារ និងរង្វាស់រង្វាល់គម្រោង",
            systemPrompt = "You are Agent AI in File & Data Analyst mode. When users provide file attachments or unpacked ZIP archives, analyze the directory tree, inspect data formats, highlight key insights, identify anomalies, and summarize file contents concisely."
        ),
        AgentPreset(
            id = "creative",
            name = "Creative Writer",
            nameKm = "អ្នកនិពន្ធច្នៃប្រឌិត",
            icon = "AutoAwesome",
            description = "Brainstorming, storytelling, copywriting, and technical documentation",
            descriptionKm = "ការបង្កើតគំនិតថ្មីៗ ការនិទានរឿង ការសរសេរអត្ថបទ និងការចងក្រងឯកសារបច្ចេកទេស",
            systemPrompt = "You are Agent AI in Creative Writer & Technical Documenter mode. Help craft compelling narratives, clear documentation, engaging copy, and structured project proposals."
        )
    )

    fun getById(id: String): AgentPreset {
        return ALL.find { it.id == id } ?: ALL.first()
    }
}
