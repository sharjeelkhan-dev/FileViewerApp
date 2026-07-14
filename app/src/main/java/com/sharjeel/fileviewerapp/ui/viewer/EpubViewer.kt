package com.sharjeel.fileviewerapp.ui.viewer

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

@Composable
fun EpubViewer(
    filePath: String,
    onZoomChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val isUp = event.changes.any { it.changedToUp() }
                        if (isUp && event.changes.size == 1) {
                            onTap()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        GestureCoordinatedBox(
            onZoomChanged = onZoomChanged,
            onTap = {} // Handled by outer Box
        ) { scale, offset ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Text(text = "Loading EPUB content...", style = MaterialTheme.typography.bodyMedium)
                    }
                    error != null -> {
                        Text(text = error ?: "Unknown Error", color = MaterialTheme.colorScheme.error)
                    }
                    htmlContent != null -> {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
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
                            shadowElevation = 12.dp
                        ) {
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        webViewClient = WebViewClient()
                                        settings.apply {
                                            javaScriptEnabled = false
                                            allowContentAccess = false
                                            allowFileAccess = false
                                        }
                                    }
                                },
                                update = { webView ->
                                    htmlContent?.let { content ->
                                        val styledHtml = """
                                        <html>
                                        <head>
                                            <style>
                                                body {
                                                    display: flex;
                                                    flex-direction: column;
                                                    align-items: center;
                                                    justify-content: center;
                                                    text-align: left;
                                                    padding: 24px;
                                                    font-family: sans-serif;
                                                    line-height: 1.7;
                                                    background-color: white;
                                                }
                                                img { max-width: 100%; height: auto; display: block; margin: 15px auto; }
                                            </style>
                                        </head>
                                        <body>$content</body>
                                        </html>
                                    """.trimIndent()
                                        webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    else -> {
                        TextViewer(
                            filePath = filePath,
                            onZoomChanged = onZoomChanged,
                            onTap = onTap
                        )
                    }
                }
            }
        }
    }
}
