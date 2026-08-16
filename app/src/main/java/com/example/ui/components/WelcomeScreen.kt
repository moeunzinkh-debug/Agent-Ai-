package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentPreset
import com.example.data.model.LocalizedContent
import com.example.ui.theme.GemmaAccent
import com.example.ui.theme.GemmaAccentLight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
    currentPreset: AgentPreset,
    language: String,
    onSuggestionClick: (String) -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val strings = remember(language) { LocalizedContent.get(language) }
    val isKm = language == "km"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Logo Badge
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GemmaAccent,
                            GemmaAccentLight,
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(GemmaAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemma 4 Agent Logo",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = strings.welcomeTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Preset Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GemmaAccent.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${if (isKm) "របៀប" else "Mode"}: ${currentPreset.getDisplayName(language)}",
                style = MaterialTheme.typography.labelMedium,
                color = GemmaAccent,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Subtitle description
        Text(
            text = currentPreset.getDisplayDescription(language),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Capabilities Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapabilityBadge(text = strings.zipInspectionBadge)
            Spacer(modifier = Modifier.width(8.dp))
            CapabilityBadge(text = strings.deepReasoningBadge)
            Spacer(modifier = Modifier.width(8.dp))
            CapabilityBadge(text = strings.codeAnalysisBadge)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Quick prompts section header
        Text(
            text = strings.suggestedPromptsTitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // Dynamic Suggestion Chips based on persona & language
        val suggestions = if (isKm) {
            when (currentPreset.id) {
                "code" -> listOf(
                    SuggestionItem("បង្កើតគំរូ Kotlin Coroutines StateFlow ស្អាត", Icons.Default.Code),
                    SuggestionItem("ដោះស្រាយបញ្ហា Memory Leaks ក្នុង Android Lifecycle", Icons.Default.Code),
                    SuggestionItem("ពិនិត្យ និងវិភាគឯកសារកូដក្នុងបណ្ណសារ ZIP", Icons.Default.FolderZip)
                )
                "reasoning" -> listOf(
                    SuggestionItem("ពន្យល់ពីគោលការណ៍ Quantum Computing និង Qubits", Icons.Default.Psychology),
                    SuggestionItem("ដោះស្រាយទ្រឹស្ដី Bayes Theorem មួយជំហានម្តងៗ", Icons.Default.Psychology),
                    SuggestionItem("ប្រៀបធៀបគុណសម្បត្តិ Monolithic vs Microservices", Icons.Default.Lightbulb)
                )
                "analyst" -> listOf(
                    SuggestionItem("សង្ខេបស្ថាបត្យកម្មទិន្នន័យ Data Architecture ទំនើប", Icons.Default.Lightbulb),
                    SuggestionItem("វិភាគទម្រង់ទិន្នន័យ API និង Model គម្រោង", Icons.Default.FolderZip),
                    SuggestionItem("ស្វែងរកចំណុចស្ទះនៃល្បឿន និង Performance Bottlenecks", Icons.Default.AutoAwesome)
                )
                else -> listOf(
                    SuggestionItem("សួស្តី! ប្រាប់ខ្ញុំពីអ្វីដែលអ្នកអាចជួយបាន", Icons.Default.AutoAwesome),
                    SuggestionItem("ពន្យល់ពី Quantum Computing មួយជំហានម្តងៗ", Icons.Default.Psychology),
                    SuggestionItem("ពិនិត្យឯកសារភ្ជាប់ និងបណ្ណសារកូដ ZIP", Icons.Default.FolderZip),
                    SuggestionItem("វិធីសាស្ត្រល្អបំផុតសម្រាប់ Kotlin Coroutines", Icons.Default.Code)
                )
            }
        } else {
            when (currentPreset.id) {
                "code" -> listOf(
                    SuggestionItem("Build a clean Kotlin Coroutine state flow template", Icons.Default.Code),
                    SuggestionItem("Debug Kotlin Android lifecycle memory leaks", Icons.Default.Code),
                    SuggestionItem("Inspect ZIP archive source files", Icons.Default.FolderZip)
                )
                "reasoning" -> listOf(
                    SuggestionItem("Explain Quantum Computing principles and algorithms", Icons.Default.Psychology),
                    SuggestionItem("Derive Bayes Theorem with step-by-step logic", Icons.Default.Psychology),
                    SuggestionItem("Compare Monolithic vs Microservices trade-offs", Icons.Default.Lightbulb)
                )
                "analyst" -> listOf(
                    SuggestionItem("Summarize data architectural patterns in 2026", Icons.Default.Lightbulb),
                    SuggestionItem("Deconstruct an API contract and data models", Icons.Default.FolderZip),
                    SuggestionItem("Analyze performance metrics and bottlenecks", Icons.Default.AutoAwesome)
                )
                else -> listOf(
                    SuggestionItem("Hello! Tell me what you can do as an AI Agent", Icons.Default.AutoAwesome),
                    SuggestionItem("Explain Quantum Computing step-by-step", Icons.Default.Psychology),
                    SuggestionItem("Inspect attached ZIP code archive", Icons.Default.FolderZip),
                    SuggestionItem("Kotlin Coroutines & Flow best practices", Icons.Default.Code)
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    item = suggestion,
                    onClick = {
                        if (suggestion.text.contains("ZIP", ignoreCase = true)) {
                            onAttachClick()
                        } else {
                            onSuggestionClick(suggestion.text)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CapabilityBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

data class SuggestionItem(
    val text: String,
    val icon: ImageVector
)

@Composable
fun SuggestionChip(
    item: SuggestionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = GemmaAccent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
