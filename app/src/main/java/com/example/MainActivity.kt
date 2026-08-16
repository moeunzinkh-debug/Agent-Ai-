package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.screens.ChatScreen
import com.example.ui.theme.GemmaAgentTheme
import com.example.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            GemmaAgentTheme(
                themeMode = uiState.settings.themeMode,
                textSize = uiState.settings.textSize
            ) {
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
