package com.sharjeel.fileviewerapp.ui.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.Firebase
import com.sharjeel.fileviewerapp.BuildConfig
import com.sharjeel.fileviewerapp.util.AIService
import com.sharjeel.fileviewerapp.util.FileUtils
import com.sharjeel.fileviewerapp.util.TextExtractionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AIViewModel @Inject constructor(
    private val aiService: AIService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIUiState>(AIUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val tag = "AIViewModel_Debug"

    // Global Exception Handler to catch unexpected failures cleanly
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = AIUiState.Error(throwable.localizedMessage ?: "An unexpected error occurred")
    }

    init {
        // App Check setup for local development as per specs
        setupAppCheckDebugProvider()
    }

    private fun setupAppCheckDebugProvider() {
        if (BuildConfig.DEBUG) {
            try {
                Firebase.appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(tag, "App Check initialized with DebugAppCheckProviderFactory successfully.")
            } catch (e: Exception) {
                Log.e(tag, "Failed to initialize App Check Debug Provider: ${e.message}", e)
            }
        }
    }

    fun summarizeFile(filePath: String) {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            val isAudio = FileUtils.isAudioFile(filePath)
            val isVideo = FileUtils.isVideoFile(filePath)
            val isImage = FileUtils.isImageFile(filePath)
            val isPdf = filePath.lowercase().endsWith(".pdf")

            if (isAudio || isVideo || isImage || isPdf) {
                _uiState.value = AIUiState.Loading("Analyzing file and generating summary...")
                try {
                    val bytes = withContext(Dispatchers.IO) { File(filePath).readBytes() }
                    val mime = FileUtils.getMimeType(filePath)
                    val result = withContext(Dispatchers.IO) {
                        aiService.summarizeMedia(bytes, mime)
                    }
                    if (!result.isNullOrBlank()) {
                        _uiState.value = AIUiState.SummaryReady(result)
                    } else {
                        _uiState.value = AIUiState.Error("Failed to generate summary content.")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Media summary failed: ${e.message}", e)
                    _uiState.value = AIUiState.Error("Analysis Failed: ${e.localizedMessage}")
                }
                return@launch
            }

            _uiState.value = AIUiState.Loading("Extracting text and generating summary...")
            val text = withContext(Dispatchers.IO) {
                TextExtractionUtils.extractText(filePath)
            } ?: ""

            if (text.isBlank()) {
                _uiState.value = AIUiState.Error("Could not read file content")
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    aiService.summarizeDocument(text)
                }

                if (!result.isNullOrBlank()) {
                    _uiState.value = AIUiState.SummaryReady(result)
                } else {
                    _uiState.value = AIUiState.Error("Failed to generate summary content.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during summarizeFile execution: ${e.message}", e)
                _uiState.value = AIUiState.Error("AI Generation Failed: ${e.localizedMessage}")
            }
        }
    }

    fun askQuestion(filePath: String, question: String) {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            if (question.isBlank()) return@launch

            val history = _chatMessages.value.takeLast(10).map { "${if (it.isUser) "User" else "AI"}: ${it.content}" }
            
            val userMsg = ChatMessage(content = question, isUser = true)
            _chatMessages.update { it + userMsg }

            val isAudio = FileUtils.isAudioFile(filePath)
            val isVideo = FileUtils.isVideoFile(filePath)
            val isImage = FileUtils.isImageFile(filePath)
            val isPdf = filePath.lowercase().endsWith(".pdf")

            if (isAudio || isVideo || isImage || isPdf) {
                try {
                    val bytes = withContext(Dispatchers.IO) { File(filePath).readBytes() }
                    val mime = FileUtils.getMimeType(filePath)
                    
                    aiService.chatWithMedia(bytes, mime, question, history).collect { chunkText ->
                        if (!chunkText.isNullOrEmpty()) {
                            _chatMessages.update { currentList ->
                                val lastMsg = currentList.lastOrNull()
                                if (lastMsg != null && !lastMsg.isUser) {
                                    currentList.dropLast(1) + lastMsg.copy(content = lastMsg.content + chunkText)
                                } else {
                                    currentList + ChatMessage(content = chunkText, isUser = false)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Media chat failure: ${e.message}", e)
                    _chatMessages.update { it + ChatMessage("Error: AI analysis failed. ${e.localizedMessage}", isUser = false) }
                }
                return@launch
            }

            val text = withContext(Dispatchers.IO) {
                TextExtractionUtils.extractText(filePath)
            } ?: ""

            if (text.isBlank()) {
                _chatMessages.update { it + ChatMessage("Error: Unable to read context from this file.", isUser = false) }
                return@launch
            }

            try {
                aiService.chatWithDocument(text, question, history).collect { chunkText ->
                    if (!chunkText.isNullOrEmpty()) {
                        _chatMessages.update { currentList ->
                            val lastMsg = currentList.lastOrNull()
                            if (lastMsg != null && !lastMsg.isUser) {
                                currentList.dropLast(1) + lastMsg.copy(content = lastMsg.content + chunkText)
                            } else {
                                currentList + ChatMessage(content = chunkText, isUser = false)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Streaming conversation failure: ${e.message}", e)
                _chatMessages.update { it + ChatMessage("Error: Stream interrupted. ${e.localizedMessage}", isUser = false) }
            }
        }
    }

    fun autoRename(filePath: String) {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            _uiState.value = AIUiState.Loading("Analyzing text for structural naming...")

            val text = withContext(Dispatchers.IO) {
                TextExtractionUtils.extractText(filePath)
            } ?: ""

            if (text.isBlank()) {
                _uiState.value = AIUiState.Error("File is empty.")
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    aiService.suggestFileNameAndCategory(text)
                }

                if (result != null) {
                    _uiState.value = AIUiState.NamingSuggestion(filePath, result.first, result.second)
                } else {
                    _uiState.value = AIUiState.Error("Could not generate naming suggestion.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Auto-naming execution crash: ${e.message}", e)
                _uiState.value = AIUiState.Error("Naming Failed: ${e.localizedMessage}")
            }
        }
    }

    fun executeGlobalCommand(prompt: String) {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            if (prompt.isBlank()) return@launch

            _uiState.value = AIUiState.Loading("Processing system action...")

            try {
                val command = withContext(Dispatchers.IO) {
                    aiService.getAppAction(prompt)
                }

                val sanitizedCommand = command
                    .replace("\"", "")
                    .replace("'", "")
                    .replace("Command:", "")
                    .replace("Action:", "")
                    .trim()

                if (sanitizedCommand == "UNKNOWN" || sanitizedCommand.isEmpty()) {
                    _uiState.value = AIUiState.Error("I'm not sure how to help with that.")
                } else {
                    _uiState.value = AIUiState.AppAction(sanitizedCommand)
                }
            } catch (e: Exception) {
                Log.e(tag, "Command orchestration crash: ${e.message}", e)
                _uiState.value = AIUiState.Error("Action parsing failure: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = AIUiState.Idle
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }
}

sealed interface AIUiState {
    data object Idle : AIUiState
    data class Loading(val message: String) : AIUiState
    data class SummaryReady(val summary: String) : AIUiState
    data class NamingSuggestion(val filePath: String, val name: String, val category: String) : AIUiState
    data class AppAction(val action: String) : AIUiState
    data class Error(val message: String) : AIUiState
}

data class ChatMessage(val content: String, val isUser: Boolean)
