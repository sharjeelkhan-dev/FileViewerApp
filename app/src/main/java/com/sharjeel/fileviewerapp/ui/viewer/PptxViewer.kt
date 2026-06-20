package com.sharjeel.fileviewerapp.ui.viewer

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

data class PptxSlide(
    val index: Int,
    val elements: List<PptxElement>
)

data class PptxElement(
    val text: String,
    val color: Color = Color.Black,
    val fontSize: Int = 16,
    val isBold: Boolean = false,
    val xPercent: Float = 0f,
    val yPercent: Float = 0f,
    val widthPercent: Float = 0.8f
)

@Composable
fun PptxViewer(filePath: String) {
    val context = LocalContext.current
    var slides by remember { mutableStateOf<List<PptxSlide>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(filePath) {
        isLoading = true
        slides = withContext(Dispatchers.IO) {
            try {
                parsePptxSlides(filePath)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Original Design View", style = MaterialTheme.typography.titleMedium)
                    Text("High-fidelity local rendering", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Button(
                    onClick = { openPptxWithExternalApp(context, filePath) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Full Edit Mode")
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                items(slides) { slide ->
                    SlideView(slide)
                }
            }
        }
    }
}

@Composable
fun SlideView(slide: PptxSlide) {
    // Standard 16:9 Slide
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 12.dp,
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEEEEEE))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val slideWidth = maxWidth
            val slideHeight = maxHeight

            // Draw text elements
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
                        lineHeight = (element.fontSize * 1.15f).sp,
                        fontWeight = if (element.isBold) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

private fun parsePptxSlides(filePath: String): List<PptxSlide> {
    val file = File(filePath)
    val result = mutableListOf<PptxSlide>()
    
    try {
        ZipFile(file).use { zip ->
            // 0. Detect Slide Size from presentation.xml
            var refW = 9144000f
            var refH = 5143500f
            
            zip.getEntry("ppt/presentation.xml")?.let { entry ->
                val dbf = DocumentBuilderFactory.newInstance()
                val db = dbf.newDocumentBuilder()
                val doc = db.parse(zip.getInputStream(entry))
                val sldSz = doc.getElementsByTagNameNS("*", "sldSz").item(0) as? org.w3c.dom.Element
                sldSz?.getAttribute("cx")?.toFloatOrNull()?.let { refW = it }
                sldSz?.getAttribute("cy")?.toFloatOrNull()?.let { refH = it }
            }

            val entries = zip.entries().asSequence()
                .filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") }
                .sortedBy { entry ->
                    val match = Regex("slide(\\d+)\\.xml").find(entry.name)
                    match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
                .toList()

            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            val db = dbf.newDocumentBuilder()

            entries.forEachIndexed { index, entry ->
                val elements = mutableListOf<PptxElement>()
                val doc = db.parse(zip.getInputStream(entry))
                
                // Get all shapes (sp)
                val shapes = doc.getElementsByTagNameNS("*", "sp")
                for (i in 0 until shapes.length) {
                    val shape = shapes.item(i) as org.w3c.dom.Element
                    
                    // 1. Get Geometry (xfrm)
                    val off = shape.getElementsByTagNameNS("*", "off").item(0) as? org.w3c.dom.Element
                    val ext = shape.getElementsByTagNameNS("*", "ext").item(0) as? org.w3c.dom.Element
                    
                    if (off == null || ext == null) continue

                    val x = off.getAttribute("x").toFloatOrNull() ?: 0f
                    val y = off.getAttribute("y").toFloatOrNull() ?: 0f
                    val w = ext.getAttribute("cx").toFloatOrNull() ?: 0f
                    
                    // 2. Get Text Body (txBody)
                    val txBody = shape.getElementsByTagNameNS("*", "txBody").item(0) as? org.w3c.dom.Element
                    if (txBody == null) continue

                    val paragraphs = txBody.getElementsByTagNameNS("*", "p")
                    val shapeText = StringBuilder()
                    var maxFontSize = 14
                    var isBold = false

                    for (j in 0 until paragraphs.length) {
                        val p = paragraphs.item(j) as org.w3c.dom.Element
                        val textRuns = p.getElementsByTagNameNS("*", "r")
                        
                        for (k in 0 until textRuns.length) {
                            val r = textRuns.item(k) as org.w3c.dom.Element
                            
                            // Text
                            val tNode = r.getElementsByTagNameNS("*", "t").item(0)
                            tNode?.let { shapeText.append(it.textContent) }

                            // Style
                            val rPr = r.getElementsByTagNameNS("*", "rPr").item(0) as? org.w3c.dom.Element
                            rPr?.let {
                                val sz = it.getAttribute("sz").toIntOrNull()
                                if (sz != null) {
                                    // 100 sz units = 1 pt
                                    val pt = sz / 100
                                    if (pt > maxFontSize) maxFontSize = pt
                                }
                                if (it.getAttribute("b") == "1") isBold = true
                            }
                        }
                        if (j < paragraphs.length - 1) shapeText.append("\n")
                    }
                    
                    val finalContent = shapeText.toString().trim()
                    if (finalContent.isNotBlank()) {
                        elements.add(PptxElement(
                            text = finalContent,
                            xPercent = (x / refW).coerceIn(0f, 1f),
                            yPercent = (y / refH).coerceIn(0f, 1f),
                            widthPercent = (w / refW).coerceIn(0.05f, 1f),
                            fontSize = maxFontSize,
                            isBold = isBold || maxFontSize >= 24 // Common title size
                        ))
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

private fun openPptxWithExternalApp(context: Context, filePath: String) {
    val file = File(filePath)
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.presentationml.presentation")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
