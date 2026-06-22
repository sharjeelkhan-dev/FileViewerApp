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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
)
{
    val context = LocalContext.current
    val exoPlayer = remember(filePath) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
            .forceEnableMediaCodecAsynchronousQueueing() // Critical for 4K/60fps to prevent blocking the playback thread

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                45_000, // Higher min buffer for 4K
                60_000, // Max buffer
                2_000,  // Buffer for playback
                5_000   // Buffer for playback after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setTunnelingEnabled(true) // Reduces CPU usage and improves A/V sync on supported hardware
                .build()
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
                setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)

                val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = false // Managed by isActive
            }
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
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            delay(500.milliseconds)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
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
            onSeek = {
                exoPlayer.seekTo(it.toLong())
                currentPosition = it.toLong()
            },
            onTogglePlay = {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            onPrevious = onPrevious,
            onNext = onNext,
            onReplay15s = { exoPlayer.seekTo((exoPlayer.currentPosition - 15000).coerceAtLeast(0)) },
            onForward15s = { exoPlayer.seekTo((exoPlayer.currentPosition + 15000).coerceAtMost(duration)) },
        )
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
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
)
{
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content
        .res.Configuration.ORIENTATION_LANDSCAPE
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Video Surface
        if (player != null) {
            key(player) {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            this.player = player
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setBackgroundColor(android.graphics.Color.BLACK)
                            (this.videoSurfaceView as? android.view.SurfaceView)?.setZOrderMediaOverlay(false)
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = {
                        it.player = player
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 24.dp, vertical = if (isLandscape) 12.dp else 40.dp)
                    .navigationBarsPadding()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Seek Bar
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = onSeek,
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        modifier = Modifier.fillMaxWidth().height(if (isLandscape) 32.dp else 40.dp),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(if (isLandscape) 16.dp else 20.dp)
                                    .background(Color.White, CircleShape)
                            )
                        },
                        track = { sliderState ->
                            val fraction = (sliderState.value - sliderState.valueRange.start) /
                                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .background(Color.White)
                                )
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text(formatTime(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 24.dp))
                    
                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(if (isLandscape) 40.dp else 48.dp)) {
                            Icon(painter = painterResource(id = R.drawable.step_backward_icon),
                                contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onReplay15s, modifier = Modifier.size(if (isLandscape) 48.dp else 56.dp)) {
                            Icon(Icons.Rounded.Replay10, contentDescription = "-15s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Surface(
                            onClick = onTogglePlay,
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(if (isLandscape) 56.dp else 72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                                )
                            }
                        }
                        IconButton(onClick = onForward15s, modifier = Modifier.size(if (isLandscape) 48.dp else 56.dp)) {
                            Icon(Icons.Rounded.Forward10, contentDescription = "+15s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(if (isLandscape) 40.dp else 48.dp)) {
                            Icon(painter = painterResource(id = R.drawable.step_forward_icon),
                                contentDescription = "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VideoViewerPreview() {
    FileViewerAppTheme {
        VideoViewerContent(
            player = null,
            isPlaying = true,
            currentPosition = 120000L,
            duration = 360000L,
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
