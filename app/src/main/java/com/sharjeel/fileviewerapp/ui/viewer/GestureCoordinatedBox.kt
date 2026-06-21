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

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale

        if (scale > 1f) {
            val extraWidth = (scale - 1) * size.width
            val extraHeight = (scale - 1) * size.height
            val maxX = extraWidth / 2
            val maxY = extraHeight / 2

            offset = Offset(
                x = (offset.x + (offsetChange.x * scale)).coerceIn(-maxX, maxX),
                y = (offset.y + (offsetChange.y * scale)).coerceIn(-maxY, maxY),
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
                       .then(
                if (scale > 1f) {
                    Modifier
                        .transformable(state = state, lockRotationOnZoomPan = true)
                        .pointerInput(scale) {
                            detectTapGestures(
                                onTap = { onTap() },
                                onDoubleTap = {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            )
                        }
                } else {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onTap() },
                            onDoubleTap = {
                                scale = 3f
                                offset = Offset.Zero
                            }
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content(scale, offset)
    }
}