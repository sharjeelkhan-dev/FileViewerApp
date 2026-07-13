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
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

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
 * Robust XML based DOCX text extractor matching textbook memory handling protocols.
 * Safely handles paragraphs and text runs without risking type cast exceptions.
 */
private fun extractTextFromDocx(filePath: String): String {
    val file = File(filePath)
    if (!file.exists()) return "Error: File does not exist."

    return try {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml")
                ?: return "Not a valid DOCX file (missing word/document.xml)"

            // Stream explicit automatic closure inside use block
            val result = zip.getInputStream(entry).use { inputStream ->
                val factory = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                }
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(inputStream)

                val pList = doc.getElementsByTagNameNS("*", "p")
                val sb = StringBuilder()

                for (i in 0 until pList.length) {
                    val pNode = pList.item(i)

                    // Direct unsafe casting avoided via Element interface assertion check
                    if (pNode.nodeType == Node.ELEMENT_NODE) {
                        val pElement = pNode as Element
                        val tList = pElement.getElementsByTagNameNS("*", "t")

                        for (j in 0 until tList.length) {
                            val textNode = tList.item(j)
                            val textContent = textNode.textContent
                            if (!textContent.isNullOrEmpty()) {
                                sb.append(textContent)
                            }
                        }
                        sb.append("\n\n")
                    }
                }
                sb.toString().trim()
            }

            if (result.isBlank()) "Empty Document" else result
        }
    } catch (e: Exception) {
        "Error reading DOCX: ${e.localizedMessage}"
    }
}