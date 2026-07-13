package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

@Composable
fun ImageViewer(
    filePath: String,
    onZoomChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current

    // Fixed: Standardize memory pipeline model targeting localized hardware rendering
    val imageRequest = remember(filePath) {
        ImageRequest.Builder(context)
            .data(File(filePath))
            .crossfade(true)
            .allowHardware(true) // Maximizes GPU decoding performance
            .dispatcher(kotlinx.coroutines.Dispatchers.IO)
            .build()
    }

    GestureCoordinatedBox(
        onZoomChanged = onZoomChanged,
        onTap = onTap
    ) { scale, offset ->
        AsyncImage(
            model = imageRequest,
            contentDescription = "Image View Portal",
            modifier = Modifier
                .fillMaxSize()
                // Fixed: Explicit lambda optimization to skip composition re-draw cycles entirely
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    clip = true // Safe boundary clipping preventing overlay spills
                },
            contentScale = ContentScale.Fit
        )
    }
}