package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

@Composable
fun DocxViewer(
    filePath: String,
    onZoomChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
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
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val isUp = event.changes.any { it.changedToUp() }
                        if (isUp && event.changes.size == 1) {
                            onTap()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        GestureCoordinatedBox(
            onZoomChanged = onZoomChanged,
            onTap = {} // Handled by outer Box
        ) { scale, offset ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 84.dp, bottom = 24.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White,
                    shadowElevation = 12.dp
                ) {
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp)
                        ) {
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2C2C2C),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
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
