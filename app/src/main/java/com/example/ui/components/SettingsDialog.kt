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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.data.model.LocalModel
import com.example.data.model.LocalizedContent
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess

@Composable
fun SettingsDialog(
    settings: AppSettings,
    activeModel: LocalModel,
    isModelDownloaded: Boolean,
    isDownloadingModel: Boolean,
    downloadPercent: Int,
    downloadedBytes: Long,
    downloadError: String?,
    onDownloadModel: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onTextSizeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
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
                Spacer(modifier = Modifier.height(8.dp))
                OnDeviceModelCard(
                    model = activeModel,
                    isDownloaded = isModelDownloaded,
                    isDownloading = isDownloadingModel,
                    downloadPercent = downloadPercent,
                    downloadedBytes = downloadedBytes,
                    downloadError = downloadError,
                    isKhmer = settings.language == "km",
                    onDownload = onDownloadModel,
                    onCancel = onCancelDownload,
                    onDelete = onDeleteModel
                )

                Spacer(modifier = Modifier.height(12.dp))


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
                            checkedTrackColor = AccentPrimary
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
                            contentColor = if (settings.isGithubConnected) StatusDanger else AccentPrimary
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
            tint = AccentPrimary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AccentPrimary,
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
                    .background(if (isSelected) AccentPrimary else Color.Transparent)
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

/**
 * Manages the GGUF weights that power real on-device inference.
 *
 * Until these weights are downloaded the app cannot answer at all — it never falls back to
 * canned text, so the download state is surfaced prominently here.
 */
@Composable
private fun OnDeviceModelCard(
    model: LocalModel,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadPercent: Int,
    downloadedBytes: Long,
    downloadError: String?,
    isKhmer: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDownloaded) StatusSuccess.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isKhmer) model.descriptionKm else model.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = model.sizeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges: uncensored / offline capability
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (model.isUncensored) {
                    ModelBadge(text = if (isKhmer) "គ្មានការត្រួតពិនិត្យ" else "Uncensored")
                }
                ModelBadge(text = if (isKhmer) "ដំណើរការក្រៅបណ្តាញ" else "Runs offline")
                ModelBadge(text = "GGUF")
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isDownloading -> {
                    Text(
                        text = if (isKhmer) "កំពុងទាញយក... $downloadPercent%"
                        else "Downloading... $downloadPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { downloadPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentPrimary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${downloadedBytes / (1024 * 1024)} MB / ${model.sizeMb} MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isKhmer) "បោះបង់" else "Cancel")
                    }
                }

                isDownloaded -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusSuccess)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isKhmer) "ត្រៀមរួចរាល់ • AI ពិតប្រាកដដំណើរការក្នុងឧបករណ៍"
                            else "Ready • real AI running on this device",
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isKhmer) "លុបម៉ូដែលចេញពីឧបករណ៍" else "Delete model from device")
                    }
                }

                else -> {
                    Text(
                        text = if (isKhmer)
                            "ត្រូវទាញយកម៉ូដែលមុនសិន។ បន្ទាប់ពីទាញយករួច AI ឆ្លើយបានទាំងគ្មានអ៊ីនធឺណិត។"
                        else
                            "Download the weights once. After that the AI answers with or without internet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isKhmer) "ទាញយកម៉ូដែល (${model.sizeLabel})"
                            else "Download model (${model.sizeLabel})"
                        )
                    }
                }
            }

            if (downloadError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = downloadError,
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusDanger
                )
            }
        }
    }
}

@Composable
private fun ModelBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentPrimary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = AccentPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

