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
    onBackClick: () -> Unit
) {
    val isFavorite by viewModel.isFavorite.collectAsState()
    val file = remember(filePath) { File(filePath) }
    val fileModel = remember(filePath) { FileModel.fromFile(file) }
    val context = androidx.compose.ui.platform.LocalContext.current

    var controlsVisible by remember { mutableStateOf(true) }
    val isAudio = fileType.lowercase() in listOf("mp3", "wav", "flac", "opus", "ogg")
    val isVideo = fileType.lowercase() in listOf("mp4", "mkv", "avi")
    val isMedia = isAudio || isVideo || fileType.lowercase() in listOf("jpg", "png", "webp", "gif")

    LaunchedEffect(filePath) {
        viewModel.checkIfFavorite(filePath)
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
                            file.name, 
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            if (isAudio || isVideo) {
                                Icon(
                                    painter = painterResource(R.drawable.house_window_icon),
                                    contentDescription = "Home",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
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
                            IconButton(onClick = { /* Info logic */ }) {
                                Icon(
                                    painter = painterResource(R.drawable.info_circle_icon),
                                    contentDescription = "Info",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = { /* More logic */ }) {
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
                            IconButton(onClick = { com.sharjeel.fileviewerapp.util.FileUtils.shareFile(context, filePath) }) {
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
                    )
                )
            }
        },
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isMedia) Color.Black else MaterialTheme.colorScheme.background)
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
                    .then(
                        if (!isMedia) {
                            Modifier.padding(innerPadding)
                        } else {
                            Modifier
                        }
                    )
            ) {
                when {
                    fileType.equals("pdf", true) -> PdfViewerScreen(filePath)
                    fileType.lowercase() in listOf("jpg", "png", "webp", "gif") -> ImageViewer(filePath)
                    isVideo -> VideoViewer(filePath, controlsVisible)
                    isAudio -> AudioViewer(filePath, controlsVisible)
                    fileType.lowercase() in listOf("txt", "csv", "json", "xml", "kt", "java", "log", "py", "js") -> TextViewer(filePath)
                    fileType.lowercase() in listOf("html", "htm") -> WebViewViewer(filePath)
                    fileType.lowercase() in listOf("docx", "doc", "xls", "xlsx", "ppt", "pptx") -> {
                        when (fileType.lowercase()) {
                            "docx" -> DocxViewer(filePath)
                            "xls", "xlsx" -> XlsxViewer(filePath)
                            else -> WebViewViewer(filePath)
                        }
                    }
                    fileType.lowercase() in listOf("epub", "mobi") -> EpubViewer(filePath)
                    else -> TextViewer(filePath)
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
            // Mocking a viewer layout
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
