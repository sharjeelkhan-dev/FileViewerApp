package com.sharjeel.fileviewerapp.util

import android.util.Log
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIService @Inject constructor(
    private val model: GenerativeModel
) {

    companion object {
        private const val TAG = "AIService"
        // Target token size safe limits roughly mapping characters (~4000 words)
        private const val MAX_CHUNK_CHARACTERS = 16000
    }

    private fun sanitizeAndChunkText(rawText: String): List<String> {
        val cleanText = rawText.replace(Regex("\\s+"), " ").trim()
        if (cleanText.length <= MAX_CHUNK_CHARACTERS) return listOf(cleanText)

        val chunks = mutableListOf<String>()
        var currentIndex = 0

        while (currentIndex < cleanText.length) {
            val endIndex = (currentIndex + MAX_CHUNK_CHARACTERS).coerceAtMost(cleanText.length)
            var slice = cleanText.substring(currentIndex, endIndex)

            // Try to break cleanly at a full sentence period or space boundary
            if (endIndex < cleanText.length) {
                val lastPeriod = slice.lastIndexOf('.')
                if (lastPeriod > MAX_CHUNK_CHARACTERS / 2) {
                    slice = slice.substring(0, lastPeriod + 1)
                }
            }
            chunks.add(slice)
            currentIndex += slice.length
        }
        return chunks
    }

    suspend fun summarizeMedia(bytes: ByteArray, mimeType: String): String? {
        Log.d(TAG, "Summarizing media. MimeType: $mimeType, Size: ${bytes.size}")
        val prompt = content {
            inlineData(bytes, mimeType)
            text(
                """
                Please provide a concise and structured summary of this media file in exactly 5-6 bullet points.
                Language Instruction:
                - Respond in the primary language detected in the content, or default to clear, simple language.
                """.trimIndent()
            )
        }
        return try {
            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            Log.e(TAG, "Error summarizing media: ${e.message}", e)
            null
        }
    }

    fun chatWithMedia(bytes: ByteArray, mimeType: String, question: String, history: List<String> = emptyList()): Flow<String?> {
        Log.d(TAG, "Chatting with media. Question: $question, History size: ${history.size}")
        val historyText = if (history.isNotEmpty()) "Conversation History:\n${history.joinToString("\n")}\n\n" else ""

        val prompt = content {
            inlineData(bytes, mimeType)
            text(
                """
                You are an expert AI assistant inside 'File Viewer App'.
                $historyText
                System Directives:
                1. Detect the language of the user's question (English, Hinglish/Roman Urdu, Urdu, Hindi, etc.).
                2. Respond strictly in the SAME language, script, and style used by the user.
                3. If the user asks about the media file, analyze it and answer accurately.
                4. If the user asks a general question, answer clearly and accurately using your general knowledge.
                
                Question: $question
                """.trimIndent()
            )
        }
        return model.generateContentStream(prompt)
            .onEach { Log.d(TAG, "Media chunk processed") }
            .map { it.text }
    }

    suspend fun summarizeDocument(text: String): String? {
        if (text.isBlank()) return null
        Log.d(TAG, "Summarizing document. Input length: ${text.length}")

        val textChunks = sanitizeAndChunkText(text)

        // If the document is small, process directly
        if (textChunks.size == 1) {
            val prompt = "Please provide a concise summary of the following document content in exactly 5-6 bullet points:\n\n${textChunks[0]}"
            return try {
                val response = model.generateContent(prompt)
                response.text
            } catch (e: Exception) {
                Log.e(TAG, "Error summarizing document: ${e.message}", e)
                null
            }
        }

        // For heavy files / long PDFs: Recursive Map-Reduce approach
        Log.d(TAG, "Large document detected. Processing ${textChunks.size} chunks sequentially.")
        val structuralSummaries = mutableListOf<String>()

        for ((index, chunk) in textChunks.withIndex()) {
            val chunkPrompt = "Summarize this segment of a large file concisely, retaining vital key data, metrics and references:\n\n$chunk"
            try {
                val response = model.generateContent(chunkPrompt)
                response.text?.let { structuralSummaries.add(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Chunk $index processing failed: ${e.message}")
            }
        }

        if (structuralSummaries.isEmpty()) return null

        val masterPrompt = "Merge the following structural context points from a single document into a single cohesive, high-quality summary consisting of exactly 5-6 bullet points:\n\n${structuralSummaries.joinToString("\n\n")}"
        return try {
            val finalResponse = model.generateContent(masterPrompt)
            finalResponse.text
        } catch (e: Exception) {
            Log.e(TAG, "Error in final reduction summary step: ${e.message}", e)
            null
        }
    }

    fun chatWithDocument(text: String, question: String, history: List<String> = emptyList()): Flow<String?> {
        Log.d(TAG, "Chatting with context. Question: $question, History size: ${history.size}")

        val workingContext = if (text.isNotBlank()) {
            val chunks = sanitizeAndChunkText(text)
            if (chunks.size > 1) chunks.take(2).joinToString("\n...\n") else chunks[0]
        } else ""

        val historyText = if (history.isNotEmpty()) "History:\n${history.joinToString("\n")}\n\n" else ""

        val prompt = if (workingContext.isBlank()) {
            """
            You are a helpful, smart AI assistant inside 'File Viewer App'.
            
            System Directives:
            1. Automatically detect the user's input language (English, Hinglish/Roman Urdu, Urdu, Hindi, etc.).
            2. Answer the question directly and accurately in the EXACT SAME language and conversational style.
            
            $historyText
            Question: $question
            """.trimIndent()
        } else {
            """
            You are an elite, highly precise context and general intelligence analyzer inside 'File Viewer App'.
            
            System Directives:
            1. Automatically detect the user's input language (English, Hinglish/Roman Urdu, Urdu, Hindi, etc.).
            2. Always reply in the EXACT SAME language and script used by the user.
            3. Prioritize using the provided Document Context to answer questions about the file.
            4. If the user asks general questions outside the context, answer accurately using your general knowledge while keeping the user's preferred language.
            
            $historyText
            Context:
            $workingContext
            
            Question: $question
            """.trimIndent()
        }

        return model.generateContentStream(prompt)
            .onEach { Log.d(TAG, "Chunk processed successfully") }
            .map { it.text }
    }

    suspend fun suggestFileNameAndCategory(content: String): Pair<String, String>? {
        if (content.isBlank()) return null
        Log.d(TAG, "Analyzing structural metrics for naming strategy.")

        val operationalSample = sanitizeAndChunkText(content).first()

        val prompt = """
            Analyze this file context slice carefully. Suggest a highly professional, contextual filename (including matching business standard extension) and classify it under exactly one of these system groups: Images, Videos, Audio, Docs, Archives.
            
            Return format must match this format strictly:
            Name: [suggested_name]
            Category: [category_group]
            
            Content: $operationalSample
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            val result = response.text ?: return null

            val lines = result.lines()
            val name = lines.firstOrNull { it.startsWith("Name:", ignoreCase = true) }
                ?.substringAfter(":")?.trim() ?: "Untitled_Document.pdf"
            val category = lines.firstOrNull { it.startsWith("Category:", ignoreCase = true) }
                ?.substringAfter(":")?.trim() ?: "Docs"

            name to category
        } catch (e: Exception) {
            Log.e(TAG, "Error suggesting structural properties: ${e.message}", e)
            null
        }
    }

    suspend fun getAppAction(prompt: String): String {
        Log.d(TAG, "Parsing system action for prompt: $prompt")
        val systemInstructions = """
            You are the AI controller for 'File Viewer App'. 
            Determine the user's intent based on their prompt in ANY language (English, Hinglish, Roman Urdu, Urdu, Hindi, etc.).
            Respond ONLY with the matched action string. No extra text, quotes, or markdown.
            
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
            
            Examples:
            - "mujhe settings me le jao" -> NAVIGATE:SETTINGS
            - "recent files dikhao" -> NAVIGATE:RECENT
            - "downloads open karo" -> NAVIGATE:DOWNLOADS
            - "search my invoices" / "invoices dhoondo" -> SEARCH:invoices
            - "favorite list dikhao" -> NAVIGATE:FAVORITES
            
            If you don't understand or it's not a direct app navigation/search command, respond strictly with UNKNOWN.
            
            User Prompt: $prompt
        """.trimIndent()

        return try {
            val response = model.generateContent(systemInstructions)
            response.text?.trim() ?: "UNKNOWN"
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing system action: ${e.message}")
            "UNKNOWN"
        }
    }
}