package com.sharjeel.fileviewerapp.ui.viewer

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import java.io.File
import androidx.core.net.toUri

@Composable
fun PdfViewerScreen(
    filePath: String,
    onTap: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                PDFView(context, null).apply {
                    fromFile(File(filePath))
                        .enableAnnotationRendering(true)
                        .enableAntialiasing(true)
                        .spacing(10)
                        .pageFitPolicy(FitPolicy.WIDTH)
                        .pageSnap(true)
                        .autoSpacing(true)
                        .pageFling(true)
                        .scrollHandle(DefaultScrollHandle(context))
                        .linkHandler { event ->
                            val uri = event.link.uri
                            if (!uri.isNullOrBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
                                    context.startActivity(intent)
                                } catch (_: Exception) {

                                }
                            }
                        }
                        .onTap {
                            onTap()
                            false // Don't consume, allow link handler to work
                        }
                        .load()
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { _ -> }
        )
    }
}
