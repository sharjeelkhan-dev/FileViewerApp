package com.sharjeel.fileviewerapp.ui.viewer

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.net.URLEncoder

@Composable
fun WebViewViewer(filePath: String) {
    val file = File(filePath)
    val extension = file.extension.lowercase()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = WebViewClient()
                
                // For Office documents, we can try to use Google Docs Viewer if it's an online URL,
                // but since these are local files, we'll try to render them directly or via a bridge.
                // For local HTML/Text/Images, WebView works directly.
                when (extension) {
                    "html", "htm", "txt" -> loadUrl("file://${file.absolutePath}")
                    "pdf" -> {
                        // We already have a PDF viewer, but WebView can also do it on some devices.
                        loadUrl("file://${file.absolutePath}")
                    }
                    else -> {
                        // Fallback: Try to render via Google Docs viewer if the device is online
                        // Note: This requires the file to be accessible via a public URL, which local files are not.
                        // For true local DOCX/XLSX rendering without external apps, 
                        // one would typically need a library like Apache POI or a specialized commercial SDK.
                        loadUrl("file://${file.absolutePath}")
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
