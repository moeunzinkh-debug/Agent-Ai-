package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentPreset
import com.example.data.model.AgentPresets
import com.example.data.model.LocalizedContent
import com.example.ui.theme.GemmaAccent
import com.example.ui.theme.GemmaAccentLight
import com.example.ui.theme.StatusSuccess

@Composable
fun TopAgentBar(
    currentPreset: AgentPreset,
    language: String,
    onPresetSelected: (AgentPreset) -> Unit,
    onLanguageToggle: (String) -> Unit,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onClearChatClick: () -> Unit,
    onShareChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var presetMenuExpanded by remember { mutableStateOf(false) }
    val strings = remember(language) { LocalizedContent.get(language) }

    // Pulsing dot animation
    val pulseAnim = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        pulseAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("menu_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = strings.closeSidebar,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            // Model Badge with Pulsing Live Dot
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GemmaAccentLight)
                    .border(1.dp, GemmaAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable { presetMenuExpanded = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(pulseAnim.value)
                            .background(StatusSuccess, CircleShape)
                    )

                    Text(
                        text = currentPreset.getDisplayName(language),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GemmaAccent
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Agent Persona",
                        tint = GemmaAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = presetMenuExpanded,
                    onDismissRequest = { presetMenuExpanded = false }
                ) {
                    AgentPresets.ALL.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = preset.getDisplayName(language),
                                    fontWeight = if (preset.id == currentPreset.id) FontWeight.Bold else FontWeight.Normal,
                                    color = if (preset.id == currentPreset.id) GemmaAccent else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onPresetSelected(preset)
                                presetMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Actions on Right: Language Toggle & Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Quick 1-Tap Language Switcher Pill (EN | ខ្មែរ)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, GemmaAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable {
                        val nextLang = if (language == "en") "km" else "en"
                        onLanguageToggle(nextLang)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = GemmaAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    AnimatedContent(
                        targetState = language,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "language_pill"
                    ) { targetLang ->
                        Text(
                            text = if (targetLang == "km") "ខ្មែរ" else "EN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GemmaAccent,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = onNewChatClick,
                modifier = Modifier.testTag("top_new_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.newChat,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onClearChatClick,
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = strings.clearChat,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onShareChatClick,
                modifier = Modifier.testTag("share_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = strings.shareChat,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = strings.settings,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
