package com.sharjeel.fileviewerapp.ui.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.Firebase
import com.sharjeel.fileviewerapp.BuildConfig
import com.sharjeel.fileviewerapp.util.TextExtractionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AIViewModel @Inject constructor(
    private val model: GenerativeModel // Hilt automatically injects the verified instance from FirebaseModule
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
            _uiState.value = AIUiState.Loading("Extracting text and generating summary...")

            val text = withContext(Dispatchers.IO) {
                TextExtractionUtils.extractText(filePath)
            } ?: ""

            if (text.isBlank()) {
                _uiState.value = AIUiState.Error("Could not read file content")
                return@launch
            }

            try {
                val prompt = "Provide a concise and structured summary of the following text:\n\n$text"

                val response = withContext(Dispatchers.IO) {
                    model.generateContent(prompt)
                }

                if (!response.text.isNullOrBlank()) {
                    _uiState.value = AIUiState.SummaryReady(response.text!!)
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

            val userMsg = ChatMessage(content = question, isUser = true)
            _chatMessages.update { it + userMsg }

            val text = withContext(Dispatchers.IO) {
                TextExtractionUtils.extractText(filePath)
            } ?: ""

            if (text.isBlank()) {
                _chatMessages.update { it + ChatMessage("Error: Unable to read context from this file.", isUser = false) }
                return@launch
            }

            try {
                val promptWithContext = "Context from file:\n$text\n\nQuestion: $question"

                model.generateContentStream(promptWithContext).collect { chunk ->
                    val chunkText = chunk.text
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
                val prompt = """
                    Analyze this text and provide an optimal filename and a category.
                    Respond ONLY in this exact format: Name, Category
                    Example response: Invoice_2026, Finance
                    
                    Text: $text
                """.trimIndent()

                val response = withContext(Dispatchers.IO) { model.generateContent(prompt) }
                val result = response.text

                if (!result.isNullOrBlank() && result.contains(",")) {
                    val parts = result.split(",", limit = 2)
                    val suggestedName = parts[0].trim()
                    val suggestedCategory = parts[1].trim()
                    _uiState.value = AIUiState.NamingSuggestion(filePath, suggestedName, suggestedCategory)
                } else {
                    _uiState.value = AIUiState.Error("Could not clean raw naming output.")
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

            val systemInstructions = """
                You are the AI controller for 'File Viewer App'. 
                Determine the most likely action based on the user's prompt.
                Respond ONLY with the action string. No extra text or quotes.
                
                Available actions:
                - NAVIGATE:HOME
                - NAVIGATE:STORAGE
                - NAVIGATE:DOWNLOADS
                - NAVIGATE:RECENT
                - NAVIGATE:FAVORITES
                - NAVIGATE:VAULT
                - NAVIGATE:TRASH
                - NAVIGATE:SETTINGS
                - SEARCH:[query]
                
                Example: NAVIGATE:RECENT or SEARCH:bills
                If you don't understand, respond with UNKNOWN.
                
                User Prompt: $prompt
            """.trimIndent()

            try {
                val response = withContext(Dispatchers.IO) { model.generateContent(systemInstructions) }
                val finalCommand = response.text?.trim() ?: "UNKNOWN"

                val sanitizedCommand = finalCommand
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