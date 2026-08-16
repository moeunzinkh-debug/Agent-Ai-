package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.FileAttachment
import com.example.data.model.LocalizedContent
import com.example.ui.theme.DarkBgSecondary
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.GemmaAccent
import com.example.ui.theme.StatusSuccess
import com.example.util.ZipAndFileHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    language: String = "en",
    isSpeaking: Boolean = false,
    onSpeakClick: (String, String) -> Unit = { _, _ -> },
    onAttachmentClick: (FileAttachment) -> Unit = {},
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val strings = remember(language) { LocalizedContent.get(language) }
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    var isThinkingExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val rotationAngle by animateFloatAsState(
        targetValue = if (isThinkingExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "thinking_arrow_rotation"
    )

    val attachments = remember(message.attachmentsJson) {
        ZipAndFileHelper.deserializeAttachments(message.attachmentsJson)
    }

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Assistant Avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(DarkBgSecondary, CircleShape)
                    .border(1.dp, GemmaAccent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = strings.modelBadge,
                    tint = GemmaAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Main Bubble Container
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) GemmaAccent else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        1.dp,
                        if (isUser) GemmaAccent else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    // Render Attachments
                    if (attachments.isNotEmpty()) {
                        attachments.forEach { file ->
                            FileAttachmentBubbleCard(
                                attachment = file,
                                onClick = { onAttachmentClick(file) },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    // Thinking Process Accordion (for assistant)
                    if (!isUser && message.thinkingProcess.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isThinkingExpanded = !isThinkingExpanded }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = GemmaAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = strings.thoughtProcessTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = GemmaAccent
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Expand thinking",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(rotationAngle)
                                )
                            }

                            AnimatedVisibility(
                                visible = isThinkingExpanded,
                                enter = fadeIn(tween(250)) + expandVertically(tween(300)),
                                exit = fadeOut(tween(200)) + shrinkVertically(tween(250))
                            ) {
                                Text(
                                    text = message.thinkingProcess,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Content Rendering
                    if (message.isThinking && message.content.isBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = strings.thinkingStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ThinkingIndicator()
                        }
                    } else {
                        FormattedMarkdownMessage(
                            text = message.content,
                            isUser = isUser,
                            onCopied = onShowToast
                        )
                    }
                }
            }

            // Bottom Timestamp & Actions
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (!isUser && message.content.isNotBlank()) {
                    // Copy Message
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            isCopied = true
                            onShowToast(strings.copiedToast)
                            scope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = strings.copyTooltip,
                            tint = if (isCopied) StatusSuccess else DarkTextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // TTS Speak
                    IconButton(
                        onClick = { onSpeakClick(message.content, message.id) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) strings.stopReadingTooltip else strings.readAloudTooltip,
                            tint = if (isSpeaking) GemmaAccent else DarkTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            // User Avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(GemmaAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FormattedMarkdownMessage(
    text: String,
    isUser: Boolean,
    onCopied: (String) -> Unit
) {
    if (text.isBlank()) return

    // Split text into code blocks and normal paragraphs
    val parts = remember(text) { parseMarkdownBlocks(text) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEach { block ->
            when (block) {
                is MarkdownBlock.Code -> {
                    CodeBlockView(
                        code = block.content,
                        language = block.language,
                        onCopied = onCopied
                    )
                }
                is MarkdownBlock.Text -> {
                    val annotated = buildMarkdownAnnotatedString(block.content, isUser)
                    Text(
                        text = annotated,
                        style = if (isUser) {
                            MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                        } else {
                            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                        },
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

sealed interface MarkdownBlock {
    data class Text(val content: String) : MarkdownBlock
    data class Code(val content: String, val language: String) : MarkdownBlock
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val codeRegex = Regex("```([a-zA-Z0-9_-]*)\n([\\s\\S]*?)```")
    var lastIndex = 0

    val matches = codeRegex.findAll(text)
    for (match in matches) {
        val start = match.range.first
        val end = match.range.last + 1

        if (start > lastIndex) {
            val textPart = text.substring(lastIndex, start).trim()
            if (textPart.isNotEmpty()) {
                blocks.add(MarkdownBlock.Text(textPart))
            }
        }

        val language = match.groupValues[1]
        val codeContent = match.groupValues[2].trimEnd()
        blocks.add(MarkdownBlock.Code(codeContent, language))
        lastIndex = end
    }

    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) {
            blocks.add(MarkdownBlock.Text(remaining))
        }
    }

    return if (blocks.isEmpty()) listOf(MarkdownBlock.Text(text)) else blocks
}

fun buildMarkdownAnnotatedString(text: String, isUser: Boolean): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line

            // Check if heading
            val isH1 = currentLine.startsWith("# ")
            val isH2 = currentLine.startsWith("## ")
            val isH3 = currentLine.startsWith("### ")

            if (isH1) currentLine = currentLine.removePrefix("# ")
            if (isH2) currentLine = currentLine.removePrefix("## ")
            if (isH3) currentLine = currentLine.removePrefix("### ")

            // Check if bullet item
            val isBullet = currentLine.startsWith("- ") || currentLine.startsWith("• ") || currentLine.startsWith("* ")
            if (isBullet) {
                append("• ")
                currentLine = currentLine.substring(2)
            }

            val style = when {
                isH1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isUser) Color.White else GemmaAccent)
                isH2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isUser) Color.White else GemmaAccent)
                isH3 -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                else -> SpanStyle()
            }

            withStyle(style) {
                // Parse bold (**text**) and inline code (`code`)
                parseInlineStyles(currentLine, isUser)
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

fun AnnotatedString.Builder.parseInlineStyles(line: String, isUser: Boolean) {
    var cursor = 0
    val regex = Regex("(\\*\\*|`)(.*?)\\1")
    val matches = regex.findAll(line)

    for (match in matches) {
        val start = match.range.first
        val end = match.range.last + 1

        if (start > cursor) {
            append(line.substring(cursor, start))
        }

        val delimiter = match.groupValues[1]
        val content = match.groupValues[2]

        if (delimiter == "**") {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(content)
            }
        } else if (delimiter == "`") {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = if (isUser) Color(0x33000000) else Color(0xFF232338),
                    color = if (isUser) Color.White else GemmaAccent,
                    fontSize = 13.sp
                )
            ) {
                append(" $content ")
            }
        }

        cursor = end
    }

    if (cursor < line.length) {
        append(line.substring(cursor))
    }
}
