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
    val file = File(filePath)
    ZipFile(file).use { zip ->
        val entry = zip.getEntry("word/document.xml") ?: return "Not a valid DOCX file"
        val xmlContent = zip.getInputStream(entry).bufferedReader().use { it.readText() }
        
        // Very basic XML tag stripping to get visible text
        val sb = StringBuilder()
        var inTag = false
        var i = 0
        while (i < xmlContent.length) {
            val c = xmlContent[i]
            if (c == '<') {
                inTag = true
                // Check for paragraph breaks
                if (xmlContent.startsWith("<w:p ", i) || xmlContent.startsWith("<w:p>", i)) {
                    sb.append("\n\n")
                }
            } else if (c == '>') {
                inTag = false
            } else if (!inTag) {
                sb.append(c)
            }
            i++
        }
        return sb.toString().trim().replace(Regex("\\n{3,}"), "\n\n")
    }
}
