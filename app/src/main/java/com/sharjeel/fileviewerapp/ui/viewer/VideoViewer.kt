package com.sharjeel.fileviewerapp.ui.viewer

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import androidx.annotation.OptIn as AOptIn
import kotlin.OptIn as KOptIn

@AOptIn(UnstableApi::class)
@Composable
fun VideoViewer(
    filePath: String,
    isVisible: Boolean,
    isActive: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current

    // Fixed: Initialized with remember to safely handle composition states,
    // while release management is strictly locked within the DisposableEffect lifecycle.
    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
            .forceEnableMediaCodecAsynchronousQueueing()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(45_000, 60_000, 2_000, 5_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters().setTunnelingEnabled(true).build()
        }

        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
                setSeekParameters(SeekParameters.CLOSEST_SYNC)
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    // Fixed: Isolate source changes securely using dynamic key extraction mapping
    LaunchedEffect(filePath) {
        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isActive
    }

    LaunchedEffect(isActive) {
        exoPlayer.playWhenReady = isActive
        if (!isActive) exoPlayer.pause()
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Fixed: Condition tracker loop executes ONLY when the player is actively streaming data pools
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                currentPosition = exoPlayer.currentPosition
                delay(250.milliseconds) // High precision tracing rate (4Hz) for stable frame-to-seek alignment
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { /* Consume events to prevent accidental pager swipes during playback */ }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap() }
    ) {
        VideoViewerContent(
            player = exoPlayer,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            isVisible = isVisible,
            onSeek = { positionFraction ->
                exoPlayer.seekTo(positionFraction.toLong())
                currentPosition = positionFraction.toLong()
            },
            onTogglePlay = {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            onPrevious = onPrevious,
            onNext = onNext,
            onReplay15s = { exoPlayer.seekTo((exoPlayer.currentPosition - 15000L).coerceAtLeast(0L)) },
            onForward15s = { exoPlayer.seekTo((exoPlayer.currentPosition + 15000L).coerceAtMost(duration)) },
        )
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatTime(millis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@AOptIn(UnstableApi::class)
@KOptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoViewerContent(
    player: Player?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isVisible: Boolean,
    onSeek: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReplay15s: () -> Unit,
    onForward15s: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (player != null) {
            key(player) {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            this.player = player
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            // Standardizing background color using Android framework surface color
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            (this.videoSurfaceView as? android.view.SurfaceView)?.setZOrderMediaOverlay(false)
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = {
                        if (it.player != player) {
                            it.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = 24.dp, vertical = if (isLandscape) 12.dp else 40.dp)
                    .navigationBarsPadding()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = onSeek,
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscape) 32.dp else 40.dp),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(if (isLandscape) 16.dp else 20.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        },
                        track = { sliderState ->
                            val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                            val fraction = if (range > 0) (sliderState.value - sliderState.valueRange.start) / range else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                        Text(formatTime(duration), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(if (isLandscape) 40.dp else 48.dp)) {
                            Icon(painter = painterResource(id = R.drawable.step_backward_icon),
                                contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onReplay15s, modifier = Modifier.size(if (isLandscape) 48.dp else 56.dp)) {
                            Icon(painter = painterResource(id = R.drawable.reset_update_icon),
                                contentDescription = "-15s",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp))
                        }
                        Surface(
                            onClick = onTogglePlay,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (isLandscape) 56.dp else 72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                                )
                            }
                        }
                        IconButton(onClick = onForward15s, modifier = Modifier.size(if (isLandscape) 48.dp else 56.dp)) {
                            Icon(painter = painterResource(id = R.drawable.forward_restore_icon),
                                contentDescription = "+15s",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(if (isLandscape) 40.dp else 48.dp)) {
                            Icon(painter = painterResource(id = R.drawable.step_forward_icon),
                                contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VideoViewerPortraitPreview() {
    FileViewerAppTheme {
        VideoViewerContent(
            player = null,
            isPlaying = false,
            currentPosition = 45000L, // 00:45
            duration = 180000L,       // 03:00
            isVisible = true,
            onSeek = {},
            onTogglePlay = {},
            onPrevious = {},
            onNext = {},
            onReplay15s = {},
            onForward15s = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VideoViewerLandscapePreview() {
    FileViewerAppTheme {
        VideoViewerContent(
            player = null,
            isPlaying = true,
            currentPosition = 120000L, // 02:00
            duration = 300000L,        // 05:00
            isVisible = true,
            onSeek = {},
            onTogglePlay = {},
            onPrevious = {},
            onNext = {},
            onReplay15s = {},
            onForward15s = {}
        )
    }
}