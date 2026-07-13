package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

@Composable
fun XlsxViewer(filePath: String) {
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

    // Fixed: Standard surface boundary setup to prevent backdrop color bleaching
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // Horizontal scrolling capability for handling wide multi-column matrices
            Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Fixed: Optimized keying structure and indexed composition loops to bypass O(N) constraints
                    itemsIndexed(
                        items = rows,
                        key = { index, _ -> index }
                    ) { rowIndex, row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEachIndexed { columnIndex, cell ->

                                // Fixed: Contextually safe theme mapping with solid high contrast layers
                                val cellBgColor = when {
                                    rowIndex == 0 -> MaterialTheme.colorScheme.primaryContainer
                                    columnIndex % 2 == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surface
                                }

                                val cellTextColor = when (rowIndex) {
                                    0 -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                val cellBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                                Surface(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(48.dp), // Unified container bounding boxes
                                    color = cellBgColor,
                                    border = BorderStroke(width = 0.5.dp, color = cellBorderColor)
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

                // Fixed: Query tags directly instead of evaluating loose multi-type child nodes
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