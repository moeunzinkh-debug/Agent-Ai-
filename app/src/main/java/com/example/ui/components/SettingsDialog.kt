package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppSettings
import com.example.data.model.LocalizedContent
import com.example.ui.theme.GemmaAccent
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess

@Composable
fun SettingsDialog(
    settings: AppSettings,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onTextSizeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onThinkingChange: (Boolean) -> Unit,
    onTtsChange: (Boolean) -> Unit,
    onCustomPromptChange: (String) -> Unit,
    onToggleGithub: () -> Unit,
    onClearAllHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var tempPrompt by remember(settings.customSystemPrompt) { mutableStateOf(settings.customSystemPrompt) }
    val strings = remember(settings.language) { LocalizedContent.get(settings.language) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancelBtn,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // LANGUAGE SECTION
                SettingsSectionHeader(title = strings.languageSection, icon = Icons.Default.Language)

                Text(
                    text = strings.languageLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                SegmentedButtons(
                    options = listOf(
                        "English" to "en",
                        "ភាសាខ្មែរ" to "km"
                    ),
                    selectedKey = settings.language,
                    onSelect = onLanguageChange
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // APPEARANCE SECTION
                SettingsSectionHeader(title = strings.appearanceSection, icon = Icons.Default.Palette)

                Text(
                    text = strings.themeModeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                SegmentedButtons(
                    options = listOf(
                        strings.themeDark to "dark",
                        strings.themeLight to "light",
                        strings.themeSystem to "system"
                    ),
                    selectedKey = settings.themeMode,
                    onSelect = onThemeChange
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.textSizeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                SegmentedButtons(
                    options = listOf(
                        strings.textSmall to "small",
                        strings.textMedium to "medium",
                        strings.textLarge to "large"
                    ),
                    selectedKey = settings.textSize,
                    onSelect = onTextSizeChange
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // INTELLIGENCE & REASONING SECTION
                SettingsSectionHeader(title = strings.intelligenceSection, icon = Icons.Default.Psychology)

                Text(
                    text = strings.coreModelLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                SegmentedButtons(
                    options = listOf(
                        "Gemma 4 Flash" to "gemma-4-flash",
                        "Gemma 4 Pro" to "gemma-4-pro",
                        "Gemini 3.5" to "gemini-3.5-flash"
                    ),
                    selectedKey = settings.activeModel,
                    onSelect = onModelChange
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.deepThinkingLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.deepThinkingDesc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isThinkingEnabled,
                        onCheckedChange = onThinkingChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GemmaAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.ttsLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.ttsDesc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isTtsEnabled,
                        onCheckedChange = onTtsChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GemmaAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = strings.customPromptLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tempPrompt,
                    onValueChange = {
                        tempPrompt = it
                        onCustomPromptChange(it)
                    },
                    placeholder = { Text(strings.customPromptPlaceholder, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // INTEGRATIONS SECTION
                SettingsSectionHeader(title = strings.integrationsSection, icon = Icons.Default.Code)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.githubLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (settings.isGithubConnected) "${strings.githubConnected} @${settings.githubUsername}" else strings.githubDisconnected,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.isGithubConnected) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleGithub,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (settings.isGithubConnected) StatusDanger else GemmaAccent
                        )
                    ) {
                        Text(text = if (settings.isGithubConnected) strings.githubDisconnectBtn else strings.githubConnectBtn)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // DATA & STORAGE
                SettingsSectionHeader(title = strings.dataStorageSection, icon = Icons.Default.Storage)

                Button(
                    onClick = { showConfirmClearDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusDanger.copy(alpha = 0.15f),
                        contentColor = StatusDanger
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.clearAllConversationsBtn, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = strings.versionFooter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text(strings.clearAllDialogTitle) },
            text = { Text(strings.clearAllDialogMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllHistory()
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text(strings.deleteConfirmBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text(strings.cancelBtn)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GemmaAccent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = GemmaAccent,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SegmentedButtons(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (label, key) ->
            val isSelected = selectedKey == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) GemmaAccent else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
