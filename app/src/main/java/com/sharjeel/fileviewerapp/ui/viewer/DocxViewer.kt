package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

@Composable
fun DocxViewer(filePath: String) {
    var content by remember { mutableStateOf("Loading...") }

    LaunchedEffect(filePath) {
        content = withContext(Dispatchers.IO) {
            try {
                extractTextFromDocx(filePath)
            } catch (e: Exception) {
                "Error rendering docx: ${e.message}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Simple docx text extractor using ZipFile (docx is a zip of XMLs).
 * We read word/document.xml and strip tags.
 */
private fun extractTextFromDocx(filePath: String): String {
    return try {
        val file = File(filePath)
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml") ?: return "Not a valid DOCX file (missing word/document.xml)"
            val inputStream = zip.getInputStream(entry)
            val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(inputStream)
            
            val sb = StringBuilder()
            val nodeList = doc.getElementsByTagNameNS("*", "t")
            for (i in 0 until nodeList.length) {
                sb.append(nodeList.item(i).textContent)
                // Add a space if needed or handle paragraphs
            }
            
            // Better approach: handle paragraphs (w:p)
            val pList = doc.getElementsByTagNameNS("*", "p")
            val result = StringBuilder()
            for (i in 0 until pList.length) {
                val p = pList.item(i)
                val tList = (p as org.w3c.dom.Element).getElementsByTagNameNS("*", "t")
                for (j in 0 until tList.length) {
                    result.append(tList.item(j).textContent)
                }
                result.append("\n\n")
            }
            
            if (result.isBlank()) {
                // Fallback to simple stripping if structured extraction failed
                sb.toString()
            } else {
                result.toString().trim()
            }
        }
    } catch (e: Exception) {
        "Error reading DOCX: ${e.localizedMessage}"
    }
}
