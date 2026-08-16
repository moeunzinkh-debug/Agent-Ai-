package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechHelper(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _currentUtteranceId = MutableStateFlow<String?>(null)
    val currentUtteranceId = _currentUtteranceId.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)
                isInitialized = true
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                _currentUtteranceId.value = utteranceId
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _currentUtteranceId.value = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentUtteranceId.value = null
            }
        })
    }

    fun speak(text: String, id: String) {
        if (!isInitialized) return
        if (_isSpeaking.value && _currentUtteranceId.value == id) {
            stop()
            return
        }
        stop()

        // Clean markdown symbols for cleaner speech
        val cleanedText = cleanMarkdownForSpeech(text)

        // Detect if text is mostly Khmer
        val hasKhmer = containsKhmer(cleanedText)
        if (hasKhmer) {
            val khmerLocale = Locale("km", "KH")
            val result = tts?.setLanguage(khmerLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale("km")
            }
        } else {
            tts?.language = Locale.US
        }

        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    private fun containsKhmer(text: String): Boolean {
        for (char in text) {
            if (char in '\u1780'..'\u17FF' || char in '\u19E0'..'\u19FF') {
                return true
            }
        }
        return false
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _currentUtteranceId.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun cleanMarkdownForSpeech(text: String): String {
        return text
            .replace(Regex("```[a-zA-Z]*\n[\\s\\S]*?```"), "Code snippet omitted.")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("[#*_~>]+"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .trim()
    }
}
