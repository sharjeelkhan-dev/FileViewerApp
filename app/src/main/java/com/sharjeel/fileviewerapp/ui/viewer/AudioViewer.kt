package com.sharjeel.fileviewerapp.ui.viewer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
fun AudioViewer(
    filePath: String,
    isVisible: Boolean,
    isActive: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
        }
    }

    LaunchedEffect(isActive) {
        exoPlayer.playWhenReady = isActive
    }

    var isPlaying by remember { mutableStateOf(value = false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(filePath) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val art = retriever.embeddedPicture
            if (art != null) {
                albumArt = BitmapFactory.decodeByteArray(art, 0, art.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
            }
        }
        exoPlayer.addListener(listener)

        while (true) {
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
            }
            delay(1000.milliseconds)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
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
        AudioViewerContent(
            albumArt = albumArt,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            isVisible = isVisible,
            isActive = isActive,
            onSeek = {
                currentPosition = it.toLong()
                exoPlayer.seekTo(it.toLong())
            },
            onTogglePlay = {
                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            onPrevious = onPrevious,
            onNext = onNext,
            onReplay15s = { exoPlayer.seekTo(currentPosition - 15000) },
            onForward15s = { exoPlayer.seekTo(currentPosition + 15000) },
        )
    }
}

@SuppressLint("DefaultLocale")
private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}

@KOptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioViewerContent(
    albumArt: Bitmap?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isVisible: Boolean,
    isActive: Boolean,
    onSeek: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReplay15s: () -> Unit,
    onForward15s: () -> Unit
) {
    // Dynamic Base Colors using MaterialTheme Tokens
    val backgroundColor = MaterialTheme.colorScheme.surface
    val onBackgroundColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        // Blurred Adaptive Background
        if (albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(40.dp).alpha(0.25f),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Album Art Container - Perfectly Centered
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(surfaceVariantColor),
                contentAlignment = Alignment.Center
            ) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.audio_tune_icon),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = onSurfaceVariantColor.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Bottom Controls Container
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor.copy(alpha = 0.95f))
                    .padding(horizontal = 24.dp, vertical = 48.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Adaptive Custom Slider
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = onSeek,
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        modifier = Modifier.fillMaxWidth(),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(primaryColor, CircleShape)
                            )
                        },
                        track = { sliderState ->
                            val fraction = (sliderState.value - sliderState.valueRange.start) /
                                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(onBackgroundColor.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .background(primaryColor)
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = onBackgroundColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = formatTime(duration),
                            color = onBackgroundColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Media Control Interface
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious) {
                            Icon(
                                painter = painterResource(id = R.drawable.step_backward_icon),
                                contentDescription = "Previous",
                                tint = onBackgroundColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onReplay15s) {
                            Icon(
                                painter = painterResource(id = R.drawable.reset_update_icon),
                                contentDescription = "-10s",
                                tint = onBackgroundColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Surface(
                            onClick = onTogglePlay,
                            shape = CircleShape,
                            color = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(64.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        IconButton(onClick = onForward15s) {
                            Icon(
                                painter = painterResource(id = R.drawable.forward_restore_icon),
                                contentDescription = "+10s",
                                tint = onBackgroundColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(
                                painter = painterResource(id = R.drawable.step_forward_icon),
                                contentDescription = "Next",
                                tint = onBackgroundColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Audio Viewer Light Mode")
@Composable
fun AudioViewerPreview() {
    FileViewerAppTheme(darkTheme = false) {
        AudioViewerContent(
            albumArt = null,
            isPlaying = true,
            currentPosition = 45000L,
            duration = 180000L,
            isVisible = true,
            isActive = true,
            onSeek = {},
            onTogglePlay = {},
            onPrevious = {},
            onNext = {},
            onReplay15s = {},
            onForward15s = {}
        )
    }
}