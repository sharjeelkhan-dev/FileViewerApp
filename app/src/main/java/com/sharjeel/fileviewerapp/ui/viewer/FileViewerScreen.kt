package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    filePath: String,
    fileType: String,
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit,
) {
    val isFavorite by viewModel.isFavorite.collectAsState()
    val playlist by viewModel.videoPlaylist.collectAsState()
    val currentIndex by viewModel.currentVideoIndex.collectAsState()

    val isVideo = fileType.lowercase() in listOf("mp4", "mkv", "avi", "webm", "3gp")

    val currentVideo = if (isVideo && currentIndex != -1 && currentIndex < playlist.size) {
        playlist[currentIndex]
    } else null

    val effectiveFilePath = currentVideo?.path ?: filePath
    val effectiveFileName = currentVideo?.name ?: File(filePath).name
    val fileModel = remember(effectiveFilePath) { FileModel.fromFile(File(effectiveFilePath)) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var controlsVisible by remember { mutableStateOf(true) }
    val isAudio = fileType.lowercase() in listOf("mp3", "wav", "flac", "opus", "ogg")
    // PDF added to media types for edge-to-edge
    val isMedia = isAudio || isVideo || fileType.lowercase() in listOf("jpg", "png", "webp", "gif", "pdf")

    LaunchedEffect(filePath) {
        viewModel.checkIfFavorite(filePath)
        viewModel.addToRecent(fileModel)
        if (isVideo) {
            viewModel.loadVideoPlaylist(filePath)
        }
    }

    LaunchedEffect(effectiveFilePath) {
        viewModel.checkIfFavorite(effectiveFilePath)
        viewModel.addToRecent(fileModel)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AnimatedVisibility(
                visible = controlsVisible || !isMedia,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            effectiveFileName,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = if (isLandscape) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick, modifier = if (isLandscape) Modifier.size(40.dp) else Modifier.size(48.dp)) {
                            if (isAudio || isVideo) {
                                Icon(
                                    painter = painterResource(R.drawable.house_window_icon),
                                    contentDescription = "Home",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    actions = {
                        if (isAudio || isVideo) {
                            IconButton(onClick = { /* Info logic */ }, modifier = if (isLandscape)
                                Modifier.size(40.dp) else Modifier.size(48.dp)) {
                                Icon(
                                    painter = painterResource(R.drawable.info_circle_icon),
                                    contentDescription = "Info",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                                )
                            }
                            IconButton(onClick = { /* More logic */ }, modifier = if (isLandscape)
                                Modifier.size(40.dp) else Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.toggleFavorite(fileModel) }) {
                                Icon(
                                    if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                            IconButton(onClick = { com.sharjeel.fileviewerapp.util.FileUtils.shareFile(context, effectiveFilePath) }) {
                                Icon(
                                    Icons.Rounded.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    windowInsets = if (isLandscape) WindowInsets(0, 0, 0, 0)
                    else TopAppBarDefaults.windowInsets
                )
            }
        },
        containerColor = if (isMedia) Color.Black else MaterialTheme.colorScheme.background,
        // Using parent Scaffold's padding bounds instead of systemBars insets directly to prevent doubling
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isMedia) Color.Black else MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isMedia) controlsVisible = !controlsVisible
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (!isMedia) innerPadding.calculateTopPadding() else 0.dp,
                            bottom = if (!isMedia) innerPadding.calculateBottomPadding() else 0.dp
                        )
                ) {
                    when {
                        fileType.equals("pdf", true) -> PdfViewerScreen(effectiveFilePath)
                        fileType.lowercase() in listOf("jpg", "png", "webp", "gif") -> ImageViewer(effectiveFilePath)
                        isVideo -> VideoViewer(
                            filePath = effectiveFilePath,
                            isVisible = controlsVisible,
                            onNext = { viewModel.playNextVideo() },
                            onPrevious = { viewModel.playPreviousVideo() }
                        )
                        isAudio -> AudioViewer(effectiveFilePath, controlsVisible)
                        fileType.lowercase() in listOf("txt", "csv", "json", "xml", "kt", "java", "log", "py", "js") -> TextViewer(effectiveFilePath)
                        fileType.lowercase() in listOf("html", "htm") -> WebViewViewer(effectiveFilePath)
                        fileType.lowercase() in listOf("docx", "doc", "xls", "xlsx", "ppt", "pptx", "docm", "xlsm", "pptm") -> {
                            when {
                                fileType.lowercase() in listOf("docx", "docm") -> DocxViewer(effectiveFilePath)
                                fileType.lowercase() in listOf("xlsx", "xlsm") -> XlsxViewer(effectiveFilePath)
                                fileType.lowercase() in listOf("pptx", "pptm") -> PptxViewer(effectiveFilePath)
                                else -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                            Text("Direct viewing of older formats ($fileType) is not yet supported.",
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(onClick = { com.sharjeel.fileviewerapp.util.FileUtils.openWithExternalApp(context, effectiveFilePath) }) {
                                                Text("Open with External App")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        fileType.lowercase() in listOf("epub", "mobi") -> EpubViewer(effectiveFilePath)
                        else -> TextViewer(effectiveFilePath)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun FileViewerPreviewLight() {
    FileViewerAppTheme(darkTheme = false) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    color = Color.Transparent
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("sample_document.pdf", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Rounded.StarBorder, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Rounded.Share, contentDescription = null)
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White), contentAlignment = Alignment.Center) {
                    Text("PDF Content Placeholder", color = Color.Gray)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun FileViewerPreviewDark() {
    FileViewerAppTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    color = Color.Transparent
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("sample_video.mp4", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White)
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}
