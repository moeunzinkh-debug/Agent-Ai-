package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.model.LocalizedContent
import com.example.ui.components.AttachmentDetailDialog
import com.example.ui.components.FileAttachmentInputChip
import com.example.ui.components.MessageBubble
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SidebarDrawer
import com.example.ui.components.ToastNotification
import com.example.ui.components.TopAgentBar
import com.example.ui.components.WelcomeScreen
import com.example.ui.theme.AccentPrimary
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentLang = uiState.settings.language
    val strings = remember(currentLang) { LocalizedContent.get(currentLang) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachmentUri(it) }
    }

    val drawerState = rememberDrawerState(
        initialValue = if (uiState.isDrawerOpen) DrawerValue.Open else DrawerValue.Closed
    )

    // Sync drawer state with viewmodel
    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen && drawerState.isClosed) {
            drawerState.open()
        } else if (!uiState.isDrawerOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen != uiState.isDrawerOpen) {
            viewModel.setDrawerOpen(drawerState.isOpen)
        }
    }

    // Auto scroll to bottom on new message
    LaunchedEffect(uiState.messages.size, uiState.isSending) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Check if scrolled up to show "Scroll to bottom" button
    val showScrollToBottom by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 3 && lastVisibleItemIndex < totalItems - 2
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                SidebarDrawer(
                    sessions = uiState.sessions,
                    activeSessionId = uiState.activeSession?.id,
                    settings = uiState.settings,
                    onSelectSession = {
                        viewModel.selectSession(it)
                        scope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        viewModel.createNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onPinSession = { viewModel.pinSession(it) },
                    onCloseSidebar = { scope.launch { drawerState.close() } },
                    onOpenSettings = {
                        viewModel.setSettingsOpen(true)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            topBar = {
                TopAgentBar(
                    currentPreset = uiState.currentPreset,
                    language = currentLang,
                    onPresetSelected = { viewModel.selectPreset(it) },
                    onLanguageToggle = { viewModel.updateLanguage(it) },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNewChatClick = { viewModel.createNewChat() },
                    onClearChatClick = { viewModel.clearCurrentChat() },
                    onShareChatClick = { viewModel.shareCurrentChat() },
                    onSettingsClick = { viewModel.setSettingsOpen(true) }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat message list or Welcome screen
                    if (uiState.messages.isEmpty()) {
                        WelcomeScreen(
                            currentPreset = uiState.currentPreset,
                            language = currentLang,
                            onSuggestionClick = { prompt ->
                                viewModel.setInputText(prompt)
                                viewModel.sendMessage()
                            },
                            onAttachClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            modifier = Modifier.weight(1f),
                            isModelReady = uiState.isModelDownloaded,
                            isDownloadingModel = uiState.isDownloadingModel,
                            downloadPercent = uiState.downloadPercent,
                            modelSizeLabel = uiState.activeModel.sizeLabel,
                            isDeviceSupported = uiState.isDeviceSupported,
                            onSetupModel = { viewModel.downloadActiveModel() }
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("chat_message_list"),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(uiState.messages, key = { it.id }) { message ->
                                    Box(modifier = Modifier.animateItem()) {
                                        MessageBubble(
                                            message = message,
                                            language = currentLang,
                                            isSpeaking = uiState.isSpeakingMessageId == message.id,
                                            onSpeakClick = { content, id ->
                                                viewModel.toggleSpeakMessage(content, id)
                                            },
                                            onAttachmentClick = { file ->
                                                viewModel.openAttachmentDetails(file)
                                            },
                                            onShowToast = { viewModel.showToast(it) }
                                        )
                                    }
                                }
                            }

                            // Smooth Scroll-to-Bottom FAB
                            if (showScrollToBottom) {
                                FloatingActionButton(
                                    onClick = {
                                        scope.launch {
                                            if (uiState.messages.isNotEmpty()) {
                                                listState.animateScrollToItem(uiState.messages.size - 1)
                                            }
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = AccentPrimary,
                                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 16.dp)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Scroll to bottom",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input & Controls Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Pending Attachments row
                        if (uiState.pendingAttachments.isNotEmpty()) {
                            val attachmentScroll = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(attachmentScroll)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.pendingAttachments.forEach { file ->
                                    FileAttachmentInputChip(
                                        attachment = file,
                                        onRemove = { viewModel.removePendingAttachment(file) }
                                    )
                                }
                            }
                        }

                        // Text Field & Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // File Attachment Button
                            IconButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("attach_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = strings.attachTooltip,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Input Box
                            OutlinedTextField(
                                value = uiState.inputText,
                                onValueChange = { viewModel.setInputText(it) },
                                placeholder = {
                                    Text(
                                        text = strings.inputPlaceholder,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field"),
                                maxLines = 4,
                                shape = RoundedCornerShape(24.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (uiState.inputText.isNotBlank() || uiState.pendingAttachments.isNotEmpty()) {
                                            viewModel.sendMessage()
                                            keyboardController?.hide()
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = AccentPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // Send Button with animated scale & color
                            val canSend = uiState.inputText.isNotBlank() || uiState.pendingAttachments.isNotEmpty()
                            val sendBtnScale by animateFloatAsState(
                                targetValue = if (canSend) 1.05f else 1.0f,
                                animationSpec = tween(150, easing = FastOutSlowInEasing),
                                label = "send_button_scale"
                            )
                            val sendBtnBg by animateColorAsState(
                                targetValue = if (canSend) AccentPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = tween(200),
                                label = "send_button_bg"
                            )

                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        viewModel.sendMessage()
                                        keyboardController?.hide()
                                    }
                                },
                                enabled = canSend && !uiState.isSending,
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(sendBtnScale)
                                    .clip(CircleShape)
                                    .background(sendBtnBg)
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = strings.sendTooltip,
                                    tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Quick Persona/Tool Switcher footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.settings.activeModel} • ${uiState.currentPreset.getDisplayName(currentLang)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Text(
                                text = if (currentLang == "km") "គាំទ្រ Markdown & កូដ" else "Markdown & Code enabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPrimary.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Toast Notification Overlay
                ToastNotification(message = uiState.toastMessage)
            }
        }
    }

    // Attachment Detail Dialog
    uiState.activeDetailAttachment?.let { attachment ->
        AttachmentDetailDialog(
            attachment = attachment,
            onDismiss = { viewModel.closeAttachmentDetails() }
        )
    }

    // Settings Dialog
    if (uiState.isSettingsOpen) {
        SettingsDialog(
            settings = uiState.settings,
            activeModel = uiState.activeModel,
            isModelDownloaded = uiState.isModelDownloaded,
            isDownloadingModel = uiState.isDownloadingModel,
            downloadPercent = uiState.downloadPercent,
            downloadedBytes = uiState.downloadedBytes,
            downloadError = uiState.downloadError,
            onDownloadModel = { viewModel.downloadActiveModel() },
            onCancelDownload = { viewModel.cancelModelDownload() },
            onDeleteModel = { viewModel.deleteActiveModel() },
            onLanguageChange = { viewModel.updateLanguage(it) },
            onThemeChange = { viewModel.updateTheme(it) },
            onTextSizeChange = { viewModel.updateTextSize(it) },
            onModelChange = { viewModel.updateModel(it) },
            onTtsChange = { viewModel.updateTts(it) },
            onCustomPromptChange = { viewModel.updateCustomPrompt(it) },
            onToggleGithub = { viewModel.toggleGithub() },
            onClearAllHistory = { viewModel.clearAllHistory() },
            onDismiss = { viewModel.setSettingsOpen(false) }
        )
    }
}
