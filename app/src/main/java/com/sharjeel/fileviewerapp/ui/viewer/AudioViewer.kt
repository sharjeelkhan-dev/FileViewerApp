package com.sharjeel.fileviewerapp.ui.viewer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn as AOptIn
import kotlin.OptIn as KOptIn
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.sharjeel.fileviewerapp.R
import androidx.compose.ui.tooling.preview.Preview
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@AOptIn(UnstableApi::class)
@Composable
fun AudioViewer(filePath: String, isVisible: Boolean) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    var isPlaying by remember { mutableStateOf(value = true) }
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
            currentPosition = exoPlayer.currentPosition
            delay(1000.milliseconds)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    AudioViewerContent(
        albumArt = albumArt,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        isVisible = isVisible,
        onSeek = { 
            currentPosition = it.toLong()
            exoPlayer.seekTo(it.toLong())
        },
        onTogglePlay = { 
            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
        },
        onPrevious = { exoPlayer.seekToPrevious() },
        onNext = { exoPlayer.seekToNext() },
        onReplay15s = { exoPlayer.seekTo(currentPosition - 15000) },
        onForward15s = { exoPlayer.seekTo(currentPosition + 15000) },
    )
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
    onSeek: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReplay15s: () -> Unit,
    onForward15s: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Blurred Background
        if (albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(40.dp).alpha(0.5f),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Album Art
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .offset(y = (-80).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray),
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
                        modifier = Modifier.size(100.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
        // Bottom Controls
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 24.dp, vertical = 70.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Seek Bar
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = onSeek,
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        modifier = Modifier.fillMaxWidth(),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium)
                        Text(formatTime(duration),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious) {
                            Icon(painter = painterResource(id = R.drawable.step_backward_icon),
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onReplay15s) {
                            Icon(Icons.Rounded.Replay10,
                                contentDescription = "-15s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp))
                        }
                        Surface(
                            onClick = onTogglePlay,
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause
                                    else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        IconButton(onClick = onForward15s) {
                            Icon(Icons.Rounded.Forward10,
                                contentDescription = "+15s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = { onNext() }) {
                            Icon(painter = painterResource(id = R.drawable.step_forward_icon),
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun AudioViewerPreview() {
    FileViewerAppTheme {
        AudioViewerContent(
            albumArt = null,
            isPlaying = true,
            currentPosition = 45000L,
            duration = 180000L,
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