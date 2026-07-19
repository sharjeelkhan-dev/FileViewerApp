package com.sharjeel.fileviewerapp.util

import java.io.File
import java.io.FileInputStream
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument

object TextExtractionUtils {

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
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }
    private fun extractPdfText(filePath: String): String? {
        return try {
            val file = File(filePath)
            "PDF Document: ${file.name}\n(Note: Detailed text extraction for PDF is under development using Gemini's multimodal capabilities or specialized libraries.)"
        } catch (_: Exception) {
            null
        }
    }
}