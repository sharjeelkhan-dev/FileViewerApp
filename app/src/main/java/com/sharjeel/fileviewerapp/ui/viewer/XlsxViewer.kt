package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

@Composable
fun XlsxViewer(
    filePath: String,
    onZoomChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    var rows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(filePath) {
        isLoading = true
        rows = withContext(Dispatchers.IO) {
            try {
                extractDataFromXlsx(filePath)
            } catch (e: Exception) {
                listOf(listOf("Error loading sheet structure", e.localizedMessage ?: "Unknown parsing issue"))
            }
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        GestureCoordinatedBox(
            onZoomChanged = onZoomChanged,
            onTap = onTap
        ) { scale, offset ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
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
                        shadowElevation = 10.dp,
                        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxHeight(),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    itemsIndexed(
                                        items = rows,
                                        key = { index, _ -> index }
                                    ) { rowIndex, row ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            row.forEachIndexed { columnIndex, cell ->
                                                val cellBgColor = when {
                                                    rowIndex == 0 -> MaterialTheme.colorScheme.primaryContainer
                                                    columnIndex % 2 == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                                    else -> MaterialTheme.colorScheme.surface
                                                }

                                                val cellTextColor = when (rowIndex) {
                                                    0 -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }

                                                Surface(
                                                    modifier = Modifier
                                                        .width(130.dp)
                                                        .height(48.dp),
                                                    color = cellBgColor,
                                                    border = BorderStroke(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                                        contentAlignment = Alignment.CenterStart
                                                    ) {
                                                        Text(
                                                            text = cell,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = cellTextColor,
                                                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
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
                }
            }
        }
    }
}

private fun extractDataFromXlsx(filePath: String): List<List<String>> {
    val file = File(filePath)
    ZipFile(file).use { zip ->
        val sharedStrings = mutableListOf<String>()
        zip.getEntry("xl/sharedStrings.xml")?.let { entry ->
            val dbf = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val doc = dbf.newDocumentBuilder().parse(zip.getInputStream(entry))
            val nodeList = doc.getElementsByTagNameNS("*", "t")
            for (i in 0 until nodeList.length) {
                sharedStrings.add(nodeList.item(i).textContent ?: "")
            }
        }

        val rows = mutableListOf<List<String>>()
        zip.getEntry("xl/worksheets/sheet1.xml")?.let { entry ->
            val dbf = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val doc = dbf.newDocumentBuilder().parse(zip.getInputStream(entry))
            val rowNodes = doc.getElementsByTagNameNS("*", "row")

            for (i in 0 until rowNodes.length) {
                val rowNode = rowNodes.item(i) as? org.w3c.dom.Element ?: continue
                val cells = mutableListOf<String>()
                val cellNodes = rowNode.getElementsByTagNameNS("*", "c")

                for (j in 0 until cellNodes.length) {
                    val cellNode = cellNodes.item(j) as? org.w3c.dom.Element ?: continue
                    val type = cellNode.getAttribute("t")
                    val vNode = cellNode.getElementsByTagNameNS("*", "v").item(0)
                    val value = vNode?.textContent ?: ""

                    if (type == "s" && value.isNotEmpty()) {
                        val idx = value.toIntOrNull()
                        cells.add(if (idx != null && idx < sharedStrings.size) sharedStrings[idx] else value)
                    } else {
                        cells.add(value)
                    }
                }
                if (cells.isNotEmpty()) {
                    rows.add(cells)
                }
            }
        }
        return rows
    }
}
