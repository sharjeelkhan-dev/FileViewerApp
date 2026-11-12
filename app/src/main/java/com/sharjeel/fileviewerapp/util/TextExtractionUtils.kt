package com.sharjeel.fileviewerapp.util

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile

object TextExtractionUtils {

    fun extractText(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            when (file.extension.lowercase()) {
                "txt", "csv", "log", "json", "xml", "kt", "java", "md", "html", "htm" -> {
                    // Safe read up to 2MB to avoid OOM
                    val maxBytes = 2 * 1024 * 1024
                    if (file.length() > maxBytes) {
                        file.inputStream().use { input ->
                            val bytes = ByteArray(maxBytes)
                            val read = input.read(bytes)
                            String(bytes, 0, read, Charsets.UTF_8)
                        }
                    } else {
                        file.readText()
                    }
                }
                "docx" -> {
                    extractDocxText(file)
                }
                "doc" -> {
                    extractDocBinaryText(file)
                }
                "pptx" -> {
                    extractPptxText(file)
                }
                "ppt" -> {
                    extractPptBinaryText(file)
                }
                "xlsx" -> {
                    extractXlsxText(file)
                }
                "xls" -> {
                    extractXlsBinaryText(file)
                }
                "pdf" -> {
                    extractPdfText(filePath)
                }
                else -> {
                    // Only read if he looks like a text file
                    if (file.length() < 1024 * 1024) {
                        try {
                            val text = file.readText()
                            if (text.isNotBlank() && text.take(500).all { it.code in 9..126 || it.code in 128..65533 || it.isWhitespace() }) {
                                text
                            } else null
                        } catch (_: Throwable) {
                            null
                        }
                    } else null
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractDocxText(file: File): String? {
        return try {
            val sb = StringBuilder()
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("word/document.xml") ?: return null
                zip.getInputStream(entry).use { inputStream ->
                    val parser = Xml.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                        setInput(inputStream, "UTF-8")
                    }

                    var eventType = parser.eventType
                    var insideText = false
                    val currentParagraph = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val tagName = parser.name?.lowercase() ?: ""
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (tagName) {
                                    "w:t", "t" -> {
                                        insideText = true
                                    }
                                    "w:p", "p" -> {
                                        currentParagraph.clear()
                                    }
                                    "w:br", "br" -> {
                                        currentParagraph.append("\n")
                                    }
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (insideText) {
                                    val text = parser.text
                                    if (!text.isNullOrEmpty()) {
                                        currentParagraph.append(text)
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (tagName == "w:t" || tagName == "t") {
                                    insideText = false
                                } else if (tagName == "w:p" || tagName == "p") {
                                    val pText = currentParagraph.toString().trim()
                                    if (pText.isNotEmpty()) {
                                        sb.append(pText).append("\n\n")
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }
            sb.toString().trim().ifEmpty { null }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractDocBinaryText(file: File): String? {
        return try {
            val bytes = file.readBytes()
            val sb = StringBuilder()
            for (b in bytes) {
                val byteVal = b.toInt() and 0xFF
                if (byteVal in 32..126 || byteVal == 10 || byteVal == 13 || byteVal == 9) {
                    sb.append(byteVal.toChar())
                }
            }
            val text = sb.toString().trim()
            if (text.length > 50) text else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractPptxText(file: File): String? {
        return try {
            val sb = StringBuilder()
            ZipFile(file).use { zip ->
                val slideEntries = zip.entries().asSequence()
                    .filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") }
                    .sortedBy { entry ->
                        Regex("slide(\\d+)\\.xml").find(entry.name)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }
                    .toList()

                for ((index, entry) in slideEntries.withIndex()) {
                    val slideTexts = mutableListOf<String>()
                    zip.getInputStream(entry).use { inputStream ->
                        val parser = Xml.newPullParser().apply {
                            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                            setInput(inputStream, "UTF-8")
                        }

                        var eventType = parser.eventType
                        var insideText = false

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            val tagName = parser.name?.lowercase() ?: ""
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    if (tagName == "a:t" || tagName == "t") {
                                        insideText = true
                                    }
                                }
                                XmlPullParser.TEXT -> {
                                    if (insideText) {
                                        val t = parser.text?.trim()
                                        if (!t.isNullOrEmpty()) {
                                            slideTexts.add(t)
                                        }
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    if (tagName == "a:t" || tagName == "t") {
                                        insideText = false
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                    }
                    if (slideTexts.isNotEmpty()) {
                        sb.append("--- Slide ${index + 1} ---\n")
                        sb.append(slideTexts.joinToString(" "))
                        sb.append("\n\n")
                    }
                }
            }
            sb.toString().trim().ifEmpty { null }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractPptBinaryText(file: File): String? {
        return try {
            val bytes = file.readBytes()
            val sb = StringBuilder()
            for (b in bytes) {
                val byteVal = b.toInt() and 0xFF
                if (byteVal in 32..126 || byteVal == 10 || byteVal == 13 || byteVal == 9) {
                    sb.append(byteVal.toChar())
                }
            }
            val text = sb.toString().trim()
            if (text.length > 50) text else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractXlsxText(file: File): String? {
        return try {
            val sb = StringBuilder()
            ZipFile(file).use { zip ->
                val sharedStrings = mutableListOf<String>()
                zip.getEntry("xl/sharedStrings.xml")?.let { entry ->
                    zip.getInputStream(entry).use { inputStream ->
                        val parser = Xml.newPullParser().apply {
                            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                            setInput(inputStream, "UTF-8")
                        }
                        var eventType = parser.eventType
                        var insideText = false
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            val name = parser.name?.lowercase() ?: ""
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    if (name == "t") insideText = true
                                }
                                XmlPullParser.TEXT -> {
                                    if (insideText) sharedStrings.add(parser.text ?: "")
                                }
                                XmlPullParser.END_TAG -> {
                                    if (name == "t") insideText = false
                                }
                            }
                            eventType = parser.next()
                        }
                    }
                }

                val sheetEntries = zip.entries().asSequence()
                    .filter { it.name.startsWith("xl/worksheets/sheet") && it.name.endsWith(".xml") }
                    .sortedBy { it.name }
                    .toList()

                for ((sheetIndex, sheetEntry) in sheetEntries.withIndex()) {
                    sb.append("--- Sheet ${sheetIndex + 1} ---\n")
                    zip.getInputStream(sheetEntry).use { inputStream ->
                        val parser = Xml.newPullParser().apply {
                            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                            setInput(inputStream, "UTF-8")
                        }
                        var eventType = parser.eventType
                        var insideValue = false
                        var cellType = ""
                        val rowCells = mutableListOf<String>()
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            val name = parser.name?.lowercase() ?: ""
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    when (name) {
                                        "c" -> {
                                            cellType = parser.getAttributeValue(null, "t") ?: ""
                                        }
                                        "v" -> {
                                            insideValue = true
                                        }
                                        "row" -> {
                                            rowCells.clear()
                                        }
                                    }
                                }
                                XmlPullParser.TEXT -> {
                                    if (insideValue) {
                                        val v = parser.text ?: ""
                                        if (cellType == "s") {
                                            val idx = v.toIntOrNull()
                                            rowCells.add(if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else v)
                                        } else {
                                            rowCells.add(v)
                                        }
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    if (name == "v") {
                                        insideValue = false
                                    } else if (name == "row") {
                                        if (rowCells.isNotEmpty()) {
                                            sb.append(rowCells.joinToString("\t")).append("\n")
                                        }
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                    }
                    sb.append("\n")
                }
            }
            sb.toString().trim().ifEmpty { null }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractXlsBinaryText(file: File): String? {
        return try {
            FileInputStream(file).use { fis ->
                val workbook = org.apache.poi.hssf.usermodel.HSSFWorkbook(fis)
                val extractor = org.apache.poi.hssf.extractor.ExcelExtractor(workbook)
                val text = extractor.text
                if (!text.isNullOrBlank()) text.trim() else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractPdfText(filePath: String): String? {
        return try {
            val file = File(filePath)
            "PDF Document: ${file.name}"
        } catch (_: Throwable) {
            null
        }
    }
}
