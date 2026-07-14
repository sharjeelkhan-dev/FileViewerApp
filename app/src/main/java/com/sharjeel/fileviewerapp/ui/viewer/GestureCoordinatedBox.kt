package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
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

    val isZoomed = scale > 1.01f

    LaunchedEffect(isZoomed) {
        onZoomChanged(isZoomed)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            // 🎯 FIXED: Proper pinch-to-zoom with centroid tracking and pager compatibility
            .pointerInput(size) {
                awaitEachGesture {
                    var zoomAccumulator = 1f
                    var panAccumulator = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop

                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)

                            if (!pastTouchSlop) {
                                zoomAccumulator *= zoomChange
                                panAccumulator += panChange

                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = abs(1 - zoomAccumulator) * centroidSize
                                val panMotion = panAccumulator.getDistance()

                                if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                val isPinching = event.changes.size > 1
                                val isZoomingAction = zoomChange != 1f
                                val isZoomedState = scale > 1.01f

                                // We only consume the gesture if we are already zoomed in
                                // OR if the user is actively starting a pinch/zoom gesture.
                                // This allows the standard 1-finger swipe to pass through to the Pager.
                                if (isZoomedState || isPinching || isZoomingAction) {
                                    val oldScale = scale
                                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                    
                                    // 🎯 FIXED: Transform centroid to be relative to the center of the box
                                    // This matches the coordinate system of 'offset' (translationX/Y)
                                    val visualCenter = Offset(size.width / 2f, size.height / 2f)
                                    val relativeCentroid = centroid - visualCenter

                                    if (newScale != oldScale || panChange != Offset.Zero) {
                                        val scaleRatio = if (oldScale != 0f) newScale / oldScale else 1f
                                        
                                        // Precise zoom formula: 
                                        // The point under the centroid should stay under the centroid after scale
                                        val targetOffset = (offset - relativeCentroid) * scaleRatio + relativeCentroid + panChange

                                        // Boundary enforcement to keep image edges within view
                                        val extraWidth = (newScale - 1f) * size.width
                                        val extraHeight = (newScale - 1f) * size.height
                                        val maxX = extraWidth / 2f
                                        val maxY = extraHeight / 2f

                                        offset = Offset(
                                            x = targetOffset.x.coerceIn(-maxX, maxX),
                                            y = targetOffset.y.coerceIn(-maxY, maxY)
                                        )
                                        scale = newScale
                                    }
                                    
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onTap() },
                onDoubleClick = {
                    if (scale > 1.01f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 3f
                        offset = Offset.Zero
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content(scale, offset)
    }
}
