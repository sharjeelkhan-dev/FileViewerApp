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
                "txt", "csv", "log", "json", "xml", "kt", "java", "md", "html", "htm" -> file.readText()
                "docx" -> {
                    try {
                        FileInputStream(file).use { fis ->
                            XWPFDocument(fis).use { doc ->
                                XWPFWordExtractor(doc).use { extractor ->
                                    extractor.text
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        null
                    }
                }
                "pdf" -> {
                    extractPdfText(filePath)
                }
                else -> {
                    try {
                        file.readText()
                    } catch (_: Throwable) {
                        null
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractPdfText(filePath: String): String? {
        return try {
            val file = File(filePath)
            "PDF Document: ${file.name}\n(Note: Detailed text extraction for PDF is under development.)"
        } catch (_: Throwable) {
            null
        }
    }
}
