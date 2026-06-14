package com.sharjeel.fileviewerapp.ui.viewer

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.PdfViewerState
import java.io.File

@Composable
fun PdfViewerScreen(filePath: String) {
    val context = LocalContext.current
    val pdfUri = Uri.fromFile(File(filePath))
    val pdfLoader = remember { SandboxedPdfLoader(context) }
    val pdfViewerState = remember { PdfViewerState() }

    val pdfDocument by produceState<PdfDocument?>(initialValue = null, pdfUri) {
        value = try {
            pdfLoader.openDocument(pdfUri)
        } catch (e: Exception) {
            null
        }
    }

    Scaffold { paddingValues ->
        pdfDocument?.let { document ->
            PdfViewer(
                pdfDocument = document,
                state = pdfViewerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
