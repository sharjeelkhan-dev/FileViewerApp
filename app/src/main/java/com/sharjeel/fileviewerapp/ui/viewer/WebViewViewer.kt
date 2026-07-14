package com.sharjeel.fileviewerapp.ui.viewer

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.io.File

@Composable
fun WebViewViewer(
    filePath: String,
    onZoomChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    val file = File(filePath)
    val extension = file.extension.lowercase()
    val isDarkTheme = isSystemInDarkTheme()

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
                                setBackgroundColor(AndroidColor.TRANSPARENT)
                                settings.apply {
                                    javaScriptEnabled = false
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                }
                                webViewClient = WebViewClient()
                                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDarkTheme)
                                }
                            }
                        },
                        update = { webView ->
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDarkTheme)
                            }

                            val targetUrl = "file://${file.absolutePath}"
                            if (webView.url != targetUrl) {
                                when (extension) {
                                    "html", "htm", "txt" -> webView.loadUrl(targetUrl)
                                    else -> {
                                        val htmlFallback = """
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                            <style>
                                                body { 
                                                    font-family: sans-serif; 
                                                    display: flex; 
                                                    flex-direction: column;
                                                    justify-content: center; 
                                                    align-items: center; 
                                                    height: 90vh; 
                                                    margin: 0;
                                                    color: #000000;
                                                    background-color: white;
                                                }
                                                .container { text-align: center; padding: 20px; }
                                            </style>
                                        </head>
                                        <body>
                                            <div class="container">
                                                <h3>Unsupported View Format</h3>
                                                <p>File: ${file.name}</p>
                                            </div>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                        webView.loadDataWithBaseURL(null, htmlFallback, "text/html", "utf-8", null)
                                    }
                                }
                            }
                        },
                        onRelease = { webView ->
                            webView.stopLoading()
                            webView.removeAllViews()
                            webView.destroy()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
