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
import org.w3c.dom.Element
import org.w3c.dom.Node

data class PptxSlide(
    val index: Int,
    val elements: List<PptxElement>
)

data class PptxElement(
    val text: String,
    val color: Color = Color.Black,
    val fontSize: Int = 14,
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

    // Fixed: Background converted to dynamic surface token to prevent high-contrast jarring borders
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Original Design View", style = MaterialTheme.typography.titleMedium)
                    Text("High-fidelity local rendering", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(slides, key = { slide -> slide.index }) { slide ->
                    SlideView(slide)
                }
            }
        }
    }
}

@Composable
fun SlideView(slide: PptxSlide) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 6.dp,
        color = Color.White, // Kept white to mimic exact presentation slide surface
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE0E0E0))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val slideWidth = maxWidth
            val slideHeight = maxHeight

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

private fun parsePptxSlides(filePath: String): List<PptxSlide> {
    val file = File(filePath)
    if (!file.exists()) return emptyList()

    val result = mutableListOf<PptxSlide>()

    try {
        ZipFile(file).use { zip ->
            var refW = 9144000f
            var refH = 5143500f

            // Safe Parsing Factory instantiation
            val dbf = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val db = dbf.newDocumentBuilder()

            zip.getEntry("ppt/presentation.xml")?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    val doc = db.parse(stream)
                    val sldSzList = doc.getElementsByTagNameNS("*", "sldSz")
                    if (sldSzList.length > 0 && sldSzList.item(0).nodeType == Node.ELEMENT_NODE) {
                        val sldSz = sldSzList.item(0) as Element
                        sldSz.getAttribute("cx")?.toFloatOrNull()?.let { refW = it }
                        sldSz.getAttribute("cy")?.toFloatOrNull()?.let { refH = it }
                    }
                }
            }

            val entries = zip.entries().asSequence()
                .filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") }
                .sortedBy { entry ->
                    val match = Regex("slide(\\d+)\\.xml").find(entry.name)
                    match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
                .toList()

            entries.forEachIndexed { index, entry ->
                val elements = mutableListOf<PptxElement>()

                // Explicit nested streaming closure
                val doc = zip.getInputStream(entry).use { stream -> db.parse(stream) }
                val shapes = doc.getElementsByTagNameNS("*", "sp")

                for (i in 0 until shapes.length) {
                    val shapeNode = shapes.item(i)
                    if (shapeNode.nodeType != Node.ELEMENT_NODE) continue
                    val shape = shapeNode as Element

                    val offList = shape.getElementsByTagNameNS("*", "off")
                    val extList = shape.getElementsByTagNameNS("*", "ext")

                    if (offList.length == 0 || extList.length == 0) continue
                    if (offList.item(0).nodeType != Node.ELEMENT_NODE || extList.item(0).nodeType != Node.ELEMENT_NODE) continue

                    val off = offList.item(0) as Element
                    val ext = extList.item(0) as Element

                    val x = off.getAttribute("x").toFloatOrNull() ?: 0f
                    val y = off.getAttribute("y").toFloatOrNull() ?: 0f
                    val w = ext.getAttribute("cx").toFloatOrNull() ?: 0f

                    val txBodyList = shape.getElementsByTagNameNS("*", "txBody")
                    if (txBodyList.length == 0 || txBodyList.item(0).nodeType != Node.ELEMENT_NODE) continue
                    val txBody = txBodyList.item(0) as Element

                    val paragraphs = txBody.getElementsByTagNameNS("*", "p")
                    val shapeText = StringBuilder()
                    var maxFontSize = 14
                    var isBold = false

                    for (j in 0 until paragraphs.length) {
                        val pNode = paragraphs.item(j)
                        if (pNode.nodeType != Node.ELEMENT_NODE) continue
                        val p = pNode as Element

                        val textRuns = p.getElementsByTagNameNS("*", "r")
                        for (k in 0 until textRuns.length) {
                            val rNode = textRuns.item(k)
                            if (rNode.nodeType != Node.ELEMENT_NODE) continue
                            val r = rNode as Element

                            val tNodeList = r.getElementsByTagNameNS("*", "t")
                            if (tNodeList.length > 0) {
                                shapeText.append(tNodeList.item(0).textContent)
                            }

                            val rPrList = r.getElementsByTagNameNS("*", "rPr")
                            if (rPrList.length > 0 && rPrList.item(0).nodeType == Node.ELEMENT_NODE) {
                                val rPr = rPrList.item(0) as Element
                                val sz = rPr.getAttribute("sz").toIntOrNull()
                                if (sz != null) {
                                    val pt = sz / 100
                                    if (pt > maxFontSize) maxFontSize = pt
                                }
                                if (rPr.getAttribute("b") == "1") isBold = true
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
                            isBold = isBold || maxFontSize >= 24
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