package com.sharjeel.fileviewerapp.ui.viewer

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.util.zip.ZipFile

@Composable
fun EpubViewer(filePath: String) {
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath) {
        try {
            // EPUB is essentially a ZIP. We look for the first HTML/XHTML file to display as a preview.
            // A full EPUB reader is complex, so we provide a "Quick View" of the content.
            val file = File(filePath)
            ZipFile(file).use { zip ->
                val entries = zip.entries().asSequence().toList()
                val contentEntry = entries.firstOrNull { 
                    it.name.endsWith(".html", true) || it.name.endsWith(".xhtml", true) 
                }
                
                if (contentEntry != null) {
                    htmlContent = zip.getInputStream(contentEntry).bufferedReader().use { it.readText() }
                } else {
                    error = "No readable content found in EPUB"
                }
            }
        } catch (e: Exception) {
            error = "Error reading EPUB: ${e.message}"
        }
    }

    if (htmlContent != null) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, htmlContent!!, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Fallback to text viewer or error message
        TextViewer(filePath)
    }
}
