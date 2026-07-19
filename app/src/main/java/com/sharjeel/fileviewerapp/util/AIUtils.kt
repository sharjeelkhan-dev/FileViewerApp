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
            text("Please provide a concise and structured summary of this media file in exactly 5-6 bullet points. Describe what you see or hear clearly.")
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
        val prompt = content {
            inlineData(bytes, mimeType)
            if (history.isNotEmpty()) {
                text("Conversation History:\n${history.joinToString("\n")}\n\n")
            }
            text("You are an expert media and document analyzer. Based on the provided file (image, audio, video, or PDF), answer the user's question precisely. If history is provided, maintain continuity.\n\nQuestion: $question")
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

        // For heavy files / long PDFs: Recursive Map-Reduce approach to squeeze core context
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

        // Final reduce pass to forge standard 5-6 bullet points layout requested by user
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
            // Squeezing dynamic boundaries to keep safe margin overheads for question tokens
            if (chunks.size > 1) chunks.take(2).joinToString("\n...\n") else chunks[0]
        } else ""

        val prompt = if (workingContext.isBlank()) {
            question
        } else {
            val historyText = if (history.isNotEmpty()) "History:\n${history.joinToString("\n")}\n\n" else ""
            "You are an elite, highly precise context analyzer. Rely ONLY on the provided content below to answer the user's question.\n\n$historyText Context:\n$workingContext\n\nQuestion: $question"
        }

        return model.generateContentStream(prompt)
            .onEach { Log.d(TAG, "Chunk processed successfully") }
            .map { it.text }
    }

    suspend fun suggestFileNameAndCategory(content: String): Pair<String, String>? {
        if (content.isBlank()) return null
        Log.d(TAG, "Analyzing structural metrics for naming strategy.")

        // Naming strategy only requires structural insights from initial document pages
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

        return try {
            val response = model.generateContent(systemInstructions)
            response.text?.trim() ?: "UNKNOWN"
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing system action: ${e.message}")
            "UNKNOWN"
        }
    }
}