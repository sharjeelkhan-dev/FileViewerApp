package com.sharjeel.fileviewerapp.ui.viewer

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

@Composable
fun EpubViewer(filePath: String) {
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // IO operations shifted to Dispatchers.IO to maintain Main Thread safety
    LaunchedEffect(filePath) {
        isLoading = true
        try {
            val file = File(filePath)
            if (!file.exists()) {
                error = "Error: File does not exist."
                isLoading = false
                return@LaunchedEffect
            }

            val extractedText = withContext(Dispatchers.IO) {
                ZipFile(file).use { zip ->
                    val entries = zip.entries().asSequence().toList()
                    val contentEntry = entries.firstOrNull {
                        it.name.endsWith(".html", true) || it.name.endsWith(".xhtml", true)
                    }

                    if (contentEntry != null) {
                        zip.getInputStream(contentEntry).bufferedReader().use { it.readText() }
                    } else {
                        null
                    }
                }
            }

            if (extractedText != null) {
                htmlContent = extractedText
            } else {
                error = "No readable HTML content found in EPUB structure."
            }
        } catch (e: Exception) {
            error = "Error reading EPUB: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Loading EPUB content...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(text = error ?: "Unknown Error", color = MaterialTheme.colorScheme.error)
                }
            }
            htmlContent != null -> {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            // XSS attacks protect karne ke liye dynamic flags disable rakhein agar basic presentation ho
                            settings.apply {
                                javaScriptEnabled = false
                                allowContentAccess = false
                                allowFileAccess = false
                            }
                        }
                    },
                    update = { webView ->
                        // Safely unwrap via state management local capture
                        htmlContent?.let { content ->
                            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                TextViewer(filePath = filePath)
            }
        }
    }
}