package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

@Composable
fun XlsxViewer(filePath: String) {
    var rows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(value = true) }

    LaunchedEffect(filePath) {
        isLoading = true
        rows = withContext(Dispatchers.IO) {
            try {
                extractDataFromXlsx(filePath)
            } catch (e: Exception) {
                listOf(listOf("Error: ${e.message}"))
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(rows) { row ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        row.forEachIndexed { index, cell ->
                            Surface(
                                modifier = Modifier.width(120.dp).padding(horizontal = 2.dp),
                                color = if ((index % 2 == 0)) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 0.5.dp, 
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                            ) {
                                Text(
                                    text = cell,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (rows.indexOf(row) == 0) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    maxLines = 2
                                )
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
        // 1. Get shared strings
        val sharedStrings = mutableListOf<String>()
        zip.getEntry("xl/sharedStrings.xml")?.let { entry ->
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(zip.getInputStream(entry))
            val nodeList = doc.getElementsByTagNameNS("*", "t")
            for (i in 0 until nodeList.length) {
                sharedStrings.add(nodeList.item(i).textContent)
            }
        }

        // 2. Get sheet1 data
        val rows = mutableListOf<List<String>>()
        zip.getEntry("xl/worksheets/sheet1.xml")?.let { entry ->
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(zip.getInputStream(entry))
            val rowNodes = doc.getElementsByTagNameNS("*", "row")
            
            for (i in 0 until rowNodes.length) {
                val rowNode = rowNodes.item(i)
                val cells = mutableListOf<String>()
                val cellNodes = rowNode.childNodes
                
                for (j in 0 until cellNodes.length) {
                    val cellNode = cellNodes.item(j)
                    if (cellNode.localName == "c" || cellNode.nodeName.endsWith(":c") || cellNode.nodeName == "c") {
                        val type = cellNode.attributes?.getNamedItem("t")?.nodeValue
                        
                        // Look for 'v' tag regardless of namespace
                        val vNode = if (cellNode is org.w3c.dom.Element) {
                             cellNode.getElementsByTagNameNS("*", "v").item(0)
                        } else null
                        
                        val value = vNode?.textContent ?: ""
                        if (type == "s" && value.isNotEmpty()) {
                            val idx = value.toIntOrNull()
                            cells.add(if (idx != null && idx < sharedStrings.size) sharedStrings[idx] else value)
                        } else {
                            cells.add(value)
                        }
                    }
                }
                rows.add(cells)
            }
        }
        return rows
    }
}
