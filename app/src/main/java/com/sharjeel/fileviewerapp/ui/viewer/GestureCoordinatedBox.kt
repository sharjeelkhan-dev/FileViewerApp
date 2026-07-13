package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

@Composable
fun GestureCoordinatedBox(
    modifier: Modifier = Modifier,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit = {},
    content: @Composable BoxScope.(scale: Float, offset: Offset) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Synchronize parent pager state instantly
    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.01f)
    }

    // Fixed: Standard mathematical pan matrix calculations based on viewport scaling thresholds
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale

        if (scale > 1f) {
            val extraWidth = (scale - 1f) * size.width
            val extraHeight = (scale - 1f) * size.height
            val maxX = extraWidth / 2f
            val maxY = extraHeight / 2f

            // Corrected Pan Vector mapping (offsetChange directly controls absolute transformation)
            offset = Offset(
                x = (offset.x + offsetChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            // Fixed: Standardized single modifier stream pipeline preventing pointer context re-allocation
            .transformable(state = state, lockRotationOnZoomPan = true)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 3f
                            offset = Offset.Zero
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content(scale, offset)
    }
}