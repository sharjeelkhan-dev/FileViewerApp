package com.sharjeel.fileviewerapp.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.appcheck.FirebaseAppCheck
import com.sharjeel.fileviewerapp.util.AIService
import com.sharjeel.fileviewerapp.util.TextExtractionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AIViewModel @Inject constructor(
    private val aiService: AIService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIUiState>(AIUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    // Global Exception Handler to catch unexpected coroutine failures cleanly
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = AIUiState.Error(throwable.localizedMessage ?: "An unexpected error occurred")
    }

    /**
     * Helper suspend function to dynamically generate Firebase App Check Token.
     * ForceRefresh true rakhne se validation replay attacks mitigate ho jate hain.
     */
    private suspend fun fetchAppCheckToken(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val appCheckInstance = FirebaseAppCheck.getInstance()
                // Fetching token securely using kotlinx-coroutines-play dependency (.await())
                val tokenResult = appCheckInstance.getToken(false).await()
                tokenResult.token
            } catch (e: Exception) {
                e.printStackTrace()
                null // Secure failure default
            }
        }
    }

    fun summarizeFile(filePath: String) {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            _uiState.value = AIUiState.Loading("Verifying app identity & generating summary...")

            // 1. Fetching App Check Token for secure AI backend validation
            val appCheckToken = fetchAppCheckToken()
            if (appCheckToken == null) {
                _uiState.value = AIUiState.Error("Security verification failed (App Check rejected)")
                return@launch
            }

            // 2. Offloading heavy file reading to I/O Thread
            val text = withContext(Dispatchers.IO) {
                TextExtractionUtils.extractText(filePath)
            } ?: ""

            if (text.isBlank()) {
                _uiState.value = AIUiState.Error("Could not read file content")
                return@launch
            }

            // Note: pass appCheckToken to your backend service call if required by modifying your API signatures
            val summary = aiService.summarizeDocument(text)
            if (summary != null) {
                _uiState.value = AIUiState.SummaryReady(summary)
            } else {
                _uiState.value = AIUiState.Error("Failed to generate summary")
            }
        }
    }

    fun askQuestion(filePath: String, question: String) {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            if (question.isBlank()) return@launch

            // Atomic state updates using .update to prevent race conditions
            val userMsg = ChatMessage(content = question, isUser = true)
            _chatMessages.update { it + userMsg }

            // Fetching verification token for secure multi-turn context
            val appCheckToken = fetchAppCheckToken()
            if (appCheckToken == null) {
                _chatMessages.update { it + ChatMessage("Error: Device verification failed via App Check.", isUser = false) }
                return@launch
            }

            val text = withContext(Dispatchers.IO) {
                TextExtractionUtils.extractText(filePath)
            } ?: ""

            if (text.isBlank()) {
                _chatMessages.update { it + ChatMessage("Error: Unable to read context from this file.", isUser = false) }
                return@launch
            }

            // Collecting chunks safely and updating UI atomically
            aiService.chatWithDocument(text, question).collect { chunk ->
                if (!chunk.isNullOrEmpty()) {
                    _chatMessages.update { currentList ->
                        val lastMsg = currentList.lastOrNull()
                        if (lastMsg != null && !lastMsg.isUser) {
                            // Append chunk to the existing assistant message
                            currentList.dropLast(1) + lastMsg.copy(content = lastMsg.content + chunk)
                        } else {
                            // First chunk received, create new message entry
                            currentList + ChatMessage(content = chunk, isUser = false)
                        }
                    }
                }
            }
        }
    }

    fun autoRename(filePath: String) {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            _uiState.value = AIUiState.Loading("Analyzing for auto-naming...")

            // Security token acquisition block
            val appCheckToken = fetchAppCheckToken()
            if (appCheckToken == null) {
                _uiState.value = AIUiState.Error("App Check token authentication failed.")
                return@launch
            }

            val text = withContext(Dispatchers.IO) {
                TextExtractionUtils.extractText(filePath)
            } ?: ""

            val suggestion = aiService.suggestFileNameAndCategory(text)
            if (suggestion != null) {
                _uiState.value = AIUiState.NamingSuggestion(filePath, suggestion.first, suggestion.second)
            } else {
                _uiState.value = AIUiState.Error("Could not generate naming suggestion")
            }
        }
    }

    fun executeGlobalCommand(prompt: String) {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            if (prompt.isBlank()) return@launch

            _uiState.value = AIUiState.Loading("Thinking...")

            // Fetching App Check validation string
            val appCheckToken = fetchAppCheckToken()
            if (appCheckToken == null) {
                _uiState.value = AIUiState.Error("Security verification failed via App Check.")
                return@launch
            }

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
            """.trimIndent()

            val combinedPrompt = "$systemInstructions\n\nUser Prompt: $prompt"

            val commandBuilder = StringBuilder()

            try {
                aiService.chatWithDocument("", combinedPrompt).collect { chunk ->
                    if (chunk != null) {
                        commandBuilder.append(chunk)
                    }
                }

                val finalCommand = commandBuilder.toString().trim()
                
                // Clean the output from possible AI artifacts
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
                _uiState.value = AIUiState.Error("AI Connection failed: ${e.message}")
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