package com.sharjeel.fileviewerapp.ui.viewer

import android.os.Build
import android.util.Xml
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipFile

data class DocxParagraph(
    val text: String,
    val isHeader: Boolean = false,
    val isBold: Boolean = false,
    val fontSizeSp: Int = 15
)

@Composable
fun DocxViewer(
    filePath: String,
    onZoomChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    var paragraphs by remember { mutableStateOf<List<DocxParagraph>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val parsed = extractDocxParagraphsSafely(filePath)
                if (parsed.isEmpty()) {
                    errorMessage = "Empty document or unreadable format."
                } else {
                    paragraphs = parsed
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error reading DOCX: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else if (errorMessage != null) {
            Text(
                text = errorMessage ?: "Error reading document",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            GestureCoordinatedBox(
                onZoomChanged = onZoomChanged,
                onTap = onTap
            ) { scale, offset ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 80.dp, bottom = 20.dp)
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
                        shadowElevation = 8.dp
                    ) {
                        SelectionContainer {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(paragraphs) { paragraph ->
                                    Text(
                                        text = paragraph.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = paragraph.fontSizeSp.sp,
                                        lineHeight = (paragraph.fontSizeSp * 1.35f).sp,
                                        fontWeight = if (paragraph.isHeader || paragraph.isBold) FontWeight.Bold else FontWeight.Normal,
                                        color = if (paragraph.isHeader) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2C),
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
    }
}

/**
 * Fast Stream Reader for Word (.docx) files matching Android native efficiency standards.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun extractDocxParagraphsSafely(filePath: String): List<DocxParagraph> {
    val file = File(filePath)
    if (!file.exists()) return emptyList()

    val paragraphList = mutableListOf<DocxParagraph>()

    try {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml") ?: return emptyList()

            zip.getInputStream(entry).use { inputStream ->
                val parser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(inputStream, "UTF-8")
                }

                var eventType = parser.eventType
                val currentParagraphText = StringBuilder()
                var insideParagraph = false
                var isBold = false
                var fontSizeHalfPoints = 28 // Default 14sp

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagName = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName != null && (tagName.equals("p", ignoreCase = true) || tagName.endsWith(":p"))) {
                                insideParagraph = true
                                currentParagraphText.clear()
                                isBold = false
                                fontSizeHalfPoints = 28
                            } else if (tagName != null && (tagName.equals("t", ignoreCase = true) || tagName.endsWith(":t")) && insideParagraph) {
                                val text = parser.nextText()
                                if (!text.isNullOrEmpty()) {
                                    currentParagraphText.append(text)
                                }
                            } else if (tagName != null && (tagName.equals("b", ignoreCase = true) || tagName.endsWith(":b"))) {
                                val valAttr = parser.getAttributeValue(null, "val")
                                if (valAttr == null || valAttr.equals("true", ignoreCase = true) || valAttr == "1") {
                                    isBold = true
                                }
                            } else if (tagName != null && (tagName.equals("sz", ignoreCase = true) || tagName.endsWith(":sz"))) {
                                val szVal = parser.getAttributeValue(null, "val")?.toIntOrNull()
                                if (szVal != null) {
                                    fontSizeHalfPoints = szVal
                                }
                            }
                        }

                        XmlPullParser.END_TAG -> {
                            if (tagName != null && (tagName.equals("p", ignoreCase = true) || tagName.endsWith(":p"))) {
                                insideParagraph = false
                                val text = currentParagraphText.toString().trim()
                                if (text.isNotEmpty()) {
                                    val fontSizeSp = (fontSizeHalfPoints / 2).coerceIn(12, 26)
                                    val isHeader = fontSizeSp >= 18 || (paragraphList.isEmpty() && fontSizeSp >= 16)

                                    paragraphList.add(
                                        DocxParagraph(
                                            text = text,
                                            isHeader = isHeader,
                                            isBold = isBold || isHeader,
                                            fontSizeSp = fontSizeSp
                                        )
                                    )
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return paragraphList
}