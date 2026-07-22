package com.sharjeel.fileviewerapp.ui.viewer

import android.util.Xml
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

data class PptxSlide(
    val index: Int,
    val elements: List<PptxElement>
)

data class PptxElement(
    val text: String,
    val color: Color = Color(0xFF1C1C1C),
    val fontSize: Int = 14,
    val isBold: Boolean = false,
    val xPercent: Float = 0f,
    val yPercent: Float = 0f,
    val widthPercent: Float = 0.8f
)

@Composable
fun PptxViewer(
    filePath: String,
    onZoomChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    var slides by remember { mutableStateOf<List<PptxSlide>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val parsed = parsePptxSlidesStream(filePath)
                if (parsed.isEmpty()) {
                    errorMessage = "No readable slide contents found."
                } else {
                    slides = parsed
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error loading PPTX: ${e.localizedMessage}"
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
                text = errorMessage ?: "Error reading presentation",
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(slides, key = { slide -> slide.index }) { slide ->
                                    PdfStyleSlidePage(slide = slide)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfStyleSlidePage(slide: PptxSlide) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 4.dp,
        color = Color.White,
        border = BorderStroke(0.5.dp, Color(0xFFD0D0D0))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val slideWidth = maxWidth
            val slideHeight = maxHeight

            // Header Indicator (Like Page Numbers in PDF)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = "${slide.index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }

            slide.elements.forEach { element ->
                val xOffset = slideWidth * element.xPercent
                val yOffset = slideHeight * element.yPercent
                val width = slideWidth * element.widthPercent

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .width(width)
                ) {
                    Text(
                        text = element.text,
                        color = element.color,
                        fontSize = element.fontSize.sp,
                        lineHeight = (element.fontSize * 1.2f).sp,
                        fontWeight = if (element.isBold) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fast & Safe Stream Parsing for PPTX
 */
private fun parsePptxSlidesStream(filePath: String): List<PptxSlide> {
    val file = File(filePath)
    if (!file.exists()) return emptyList()

    val result = mutableListOf<PptxSlide>()

    try {
        ZipFile(file).use { zip ->
            val refW = 9144000f
            val refH = 5143500f

            val entries = zip.entries().asSequence()
                .filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") }
                .sortedBy { entry ->
                    val match = Regex("slide(\\d+)\\.xml").find(entry.name)
                    match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
                .toList()

            entries.forEachIndexed { index, entry ->
                val elements = mutableListOf<PptxElement>()

                zip.getInputStream(entry).use { inputStream ->
                    val parser = Xml.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                        setInput(inputStream, "UTF-8")
                    }

                    var eventType = parser.eventType
                    val currentText = StringBuilder()
                    var x = 0f
                    var y = 0f
                    var w = 0f
                    var maxFontSize = 14
                    var isBold = false
                    var insideShape = false

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val tagName = parser.name
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (tagName != null && (tagName.equals("sp", ignoreCase = true) || tagName.endsWith(":sp"))) {
                                    insideShape = true
                                    currentText.clear()
                                    x = 0f
                                    y = 0f
                                    w = 0f
                                    maxFontSize = 14
                                    isBold = false
                                } else if (tagName != null && (tagName.equals("off", ignoreCase = true) || tagName.endsWith(":off"))) {
                                    x = parser.getAttributeValue(null, "x")?.toFloatOrNull() ?: 0f
                                    y = parser.getAttributeValue(null, "y")?.toFloatOrNull() ?: 0f
                                } else if (tagName != null && (tagName.equals("ext", ignoreCase = true) || tagName.endsWith(":ext"))) {
                                    w = parser.getAttributeValue(null, "cx")?.toFloatOrNull() ?: 0f
                                } else if (tagName != null && (tagName.equals("t", ignoreCase = true) || tagName.endsWith(":t")) && insideShape) {
                                    val text = parser.nextText()
                                    if (!text.isNullOrEmpty()) {
                                        currentText.append(text)
                                    }
                                } else if (tagName != null && (tagName.equals("rPr", ignoreCase = true) || tagName.endsWith(":rPr"))) {
                                    val sz = parser.getAttributeValue(null, "sz")?.toIntOrNull()
                                    if (sz != null) {
                                        val pt = sz / 100
                                        if (pt > maxFontSize) maxFontSize = pt
                                    }
                                    val b = parser.getAttributeValue(null, "b")
                                    if (b == "1" || b.equals("true", ignoreCase = true)) {
                                        isBold = true
                                    }
                                }
                            }

                            XmlPullParser.END_TAG -> {
                                if (tagName != null && (tagName.equals("sp", ignoreCase = true) || tagName.endsWith(":sp"))) {
                                    insideShape = false
                                    val content = currentText.toString().trim()
                                    if (content.isNotEmpty()) {
                                        elements.add(
                                            PptxElement(
                                                text = content,
                                                xPercent = (x / refW).coerceIn(0f, 1f),
                                                yPercent = (y / refH).coerceIn(0f, 1f),
                                                widthPercent = (w / refW).coerceIn(0.05f, 1f),
                                                fontSize = maxFontSize,
                                                isBold = isBold || maxFontSize >= 22
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
                result.add(PptxSlide(index, elements))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return result
}