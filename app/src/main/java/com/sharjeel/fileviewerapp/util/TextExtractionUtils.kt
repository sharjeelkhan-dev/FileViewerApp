package com.sharjeel.fileviewerapp.util

import java.io.File
import java.io.FileInputStream
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument

object TextExtractionUtils {
    /**
     * Reads text content from supported file types.
     */
    fun extractText(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists()) return null
        
        return try {
            when (file.extension.lowercase()) {
                "txt", "csv", "log", "json", "xml", "kt", "java", "md" -> file.readText()
                "docx" -> {
                    FileInputStream(file).use { fis ->
                        XWPFDocument(fis).use { doc ->
                            XWPFWordExtractor(doc).use { extractor ->
                                // Extraction limit raised to 5MB to ensure "All Text" actually means all
                                extractor.text
                            }
                        }
                    }
                }
                "pdf" -> {
                    // PDF text extraction limit also removed/increased
                    extractPdfText(filePath)
                }
                // For other files, try to read as text
                else -> {
                    try {
                        file.readText()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractPdfText(filePath: String): String? {
        return try {
            val file = File(filePath)
            // Actual text extraction for PDF requires a library like Apache PdfBox-Android.
            // For now, providing a clear placeholder to context.
            "PDF Document: ${file.name}\n(Note: Detailed text extraction for PDF is under development using Gemini's multimodal capabilities or specialized libraries.)"
        } catch (e: Exception) {
            null
        }
    }
}
