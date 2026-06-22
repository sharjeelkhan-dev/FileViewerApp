package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import androidx.compose.ui.tooling.preview.Preview
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    filePath: String,
    fileType: String,
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit,
    onShowInFolder: (String) -> Unit,
) {
    val isFavorite by viewModel.isFavorite.collectAsState()
    val filePlaylist by viewModel.filePlaylist.collectAsState()
    val currentFileIndex by viewModel.currentFileIndex.collectAsState()

    // Determine content based on currently active file in the pager
    val currentFile = if (currentFileIndex != -1 && currentFileIndex < filePlaylist.size) {
        filePlaylist[currentFileIndex]
    } else null

    val effectiveFilePath = currentFile?.path ?: filePath
    val fileModel = remember(effectiveFilePath) { FileModel.fromFile(File(effectiveFilePath)) }

    LaunchedEffect(filePath) {
        viewModel.checkIfFavorite(filePath)
        viewModel.addToRecent(fileModel)
        viewModel.loadFolderPlaylist(filePath)
    }

    LaunchedEffect(effectiveFilePath) {
        viewModel.checkIfFavorite(effectiveFilePath)
        viewModel.addToRecent(fileModel)
    }

    FileViewerContent(
        filePath = filePath,
        fileType = fileType,
        isFavorite = isFavorite,
        filePlaylist = filePlaylist,
        currentFileIndex = currentFileIndex,
        onBackClick = onBackClick,
        onToggleFavorite = { viewModel.toggleFavorite(fileModel) },
        onUpdateFileIndex = { viewModel.updateFileIndex(it) },
        onShowInFolder = onShowInFolder
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileViewerContent(
    filePath: String,
    fileType: String,
    isFavorite: Boolean,
    filePlaylist: List<FileModel>,
    currentFileIndex: Int,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateFileIndex: (Int) -> Unit,
    onShowInFolder: (String) -> Unit,
) {
    // Determine content based on currently active file in the pager
    val currentFile = if (currentFileIndex != -1 && currentFileIndex < filePlaylist.size) {
        filePlaylist[currentFileIndex]
    } else null

    val effectiveFilePath = currentFile?.path ?: filePath
    val effectiveFileName = currentFile?.name ?: File(filePath).name
    val effectiveFileType = currentFile?.extension?.lowercase() ?: fileType.lowercase()
    val fileModel = remember(effectiveFilePath) { FileModel.fromFile(File(effectiveFilePath)) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var controlsVisible by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    val isAudio = effectiveFileType in listOf("mp3", "wav", "flac", "opus", "ogg")
    val isVideo = effectiveFileType in listOf("mp4", "mkv", "avi", "webm", "3gp")
    val isImage = effectiveFileType in listOf("jpg", "png", "webp", "gif", "jpeg")
    val isPdf = effectiveFileType == "pdf"
    val isMedia = isAudio || isVideo || isImage || isPdf

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
                            IconButton(onClick = { showMenu = true }, modifier = if (isLandscape)
                                Modifier.size(40.dp) else Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.White
                                )

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        onClick = {
                                            showMenu = false
                                            com.sharjeel.fileviewerapp.util.FileUtils.shareFile(context, effectiveFilePath)
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open with") },
                                        onClick = {
                                            showMenu = false
                                            com.sharjeel.fileviewerapp.util.FileUtils.openWithExternalApp(context, effectiveFilePath)
                                        },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Show in Folder") },
                                        onClick = {
                                            showMenu = false
                                            onShowInFolder(File(effectiveFilePath).parent ?: "")
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites") },
                                        onClick = {
                                            showMenu = false
                                            onToggleFavorite()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                                contentDescription = null,
                                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = onToggleFavorite) {
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
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (!isMedia) innerPadding.calculateTopPadding() else 0.dp,
                            bottom = if (!isMedia) innerPadding.calculateBottomPadding() else 0.dp
                        )
                ) {
                    if (filePlaylist.isNotEmpty()) {
                        UniversalFilePager(
                            filePlaylist = filePlaylist,
                            initialIndex = filePlaylist.indexOfFirst { it.path == effectiveFilePath }.coerceAtLeast(0),
                            controlsVisible = controlsVisible,
                            onToggleControls = { controlsVisible = !controlsVisible },
                            onFileChanged = { /* Handled by effectiveFilePath LaunchEffect */ },
                            onIndexChanged = onUpdateFileIndex
                        )
                    } else {
                        // Immediate rendering while playlist loads
                        FileContentRenderer(
                            file = fileModel,
                            controlsVisible = controlsVisible,
                            isActive = true,
                            onZoomChanged = {},
                            onToggleControls = { controlsVisible = !controlsVisible },
                            onNext = {},
                            onPrevious = {}
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FileViewerScreenPreview() {
    FileViewerAppTheme {
        FileViewerContent(
            filePath = "/path/img.jpg",
            fileType = "jpg",
            isFavorite = true,
            filePlaylist = listOf(
                FileModel("Vacation.jpg", "/path/img.jpg", 2 * 1024 * 1024L, System.currentTimeMillis(), false, extension = "jpg"),
                FileModel("Contract.pdf", "/path/doc.pdf", 500 * 1024L, System.currentTimeMillis(), false, extension = "pdf")
            ),
            currentFileIndex = 0,
            onBackClick = {},
            onToggleFavorite = {},
            onUpdateFileIndex = {},
            onShowInFolder = {}
        )
    }
}