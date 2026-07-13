package com.sharjeel.fileviewerapp.ui.viewer

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.io.File

@Composable
fun WebViewViewer(filePath: String) {
    val file = File(filePath)
    val extension = file.extension.lowercase()
    val isDarkTheme = isSystemInDarkTheme()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // Fixed: Explicit background binding to eliminate flash frames on low spec devices
                setBackgroundColor(AndroidColor.TRANSPARENT)

                settings.apply {
                    // Fixed: Absolute Security Context Isolation Policy
                    javaScriptEnabled = false // Disabled by default for local offline file parsing safety
                    allowFileAccess = true
                    allowContentAccess = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                webViewClient = WebViewClient()

                // Fixed: Dynamic Theme Inject Engine for Android WebKit Components
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDarkTheme)
                }
            }
        },
        update = { webView ->
            // Fixed: Realtime synchronization mapping during runtime composition triggers
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDarkTheme)
            }

            val targetUrl = "file://${file.absolutePath}"
            if (webView.url != targetUrl) {
                when (extension) {
                    "html", "htm", "txt" -> webView.loadUrl(targetUrl)
                    else -> {
                        // Fixed: Safe string boundary wrapping for unsupported office/binary files fallback stream
                        val htmlFallback = """
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    body { 
                                        font-family: sans-serif; 
                                        display: flex; 
                                        justify-content: center; 
                                        align-items: center; 
                                        height: 90vh; 
                                        margin: 0;
                                        color: ${if (isDarkTheme) "#FFFFFF" else "#000000"};
                                        background-color: transparent;
                                    }
                                    .container { text-align: center; padding: 20px; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <h3>Unsupported View Format</h3>
                                    <p>File: ${file.name}</p>
                                    <p>Use external app selector pipeline to parse this binary stream.</p>
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
            // Fixed: Prevent system frame references leak inside virtual layout nodes
            webView.stopLoading()
            webView.removeAllViews()
            webView.destroy()
        },
        modifier = Modifier.fillMaxSize()
    )
}