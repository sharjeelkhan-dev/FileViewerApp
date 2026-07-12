package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import androidx.compose.ui.tooling.preview.Preview
import com.sharjeel.fileviewerapp.ui.ai.AIViewModel
import com.sharjeel.fileviewerapp.ui.ai.AIUiState
import com.sharjeel.fileviewerapp.ui.components.AppScaffold
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import com.sharjeel.fileviewerapp.ui.theme.NeonPrimary
import java.io.File
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    filePath: String,
    fileType: String,
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit,
    onShowInFolder: (String) -> Unit,
    aiViewModel: AIViewModel = hiltViewModel(),
) {
    val isFavorite by viewModel.isFavorite.collectAsState()
    val filePlaylist by viewModel.filePlaylist.collectAsState()
    val currentFileIndex by viewModel.currentFileIndex.collectAsState()
    val aiUiState by aiViewModel.uiState.collectAsState()
    val aiMessages by aiViewModel.chatMessages.collectAsState()

    // ... rest of logic
    val currentFile = if ((currentFileIndex != -1) && (currentFileIndex < filePlaylist.size)) {
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
        onShowInFolder = onShowInFolder,
        aiUiState = aiUiState,
        aiMessages = aiMessages,
        onSummarize = { aiViewModel.summarizeFile(effectiveFilePath) },
    ) {
        aiViewModel.askQuestion(effectiveFilePath, it)
    }
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
    aiUiState: AIUiState,
    aiMessages: List<com.sharjeel.fileviewerapp.ui.ai.ChatMessage>,
    onSummarize: () -> Unit,
    onAskAI: (String) -> Unit
) {
    // ... rest of code
    val currentFile = if ((currentFileIndex != -1) && (currentFileIndex < filePlaylist.size)) {
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

    var showAISummary by remember { mutableStateOf(false) }

    if (showAISummary) {
        ModalBottomSheet(
            onDismissRequest = { showAISummary = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "AI Smart Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                var userQuestion by remember { mutableStateOf("") }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        when (val state = aiUiState) {
                            is AIUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            is AIUiState.SummaryReady -> Text(state.summary, style = MaterialTheme.typography.bodyMedium)
                            is AIUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                            else -> Text("Generating summary...")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(aiMessages) { msg ->
                        Surface(
                            color = if (msg.isUser) NeonPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.align(if (msg.isUser) Alignment.End else Alignment.Start)
                        ) {
                            Text(
                                msg.content,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = userQuestion,
                    onValueChange = { userQuestion = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ask about this document...") },
                    trailingIcon = {
                        IconButton(onClick = { 
                            if (userQuestion.isNotBlank()) {
                                onAskAI(userQuestion)
                                userQuestion = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = NeonPrimary)
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }

    AppScaffold(
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
                                    text = { Text("Add to Favorites") },
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
                                
                                if (isPdf || effectiveFileType == "txt") {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    
                                    DropdownMenuItem(
                                        text = { Text("AI Summarize", color = NeonPrimary, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showMenu = false
                                            onSummarize()
                                            showAISummary = true
                                        },
                                        leadingIcon = { Icon(painterResource(R.drawable.brush_paintbrush_icon), contentDescription = null, tint = NeonPrimary, modifier = Modifier.size(24.dp)) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Chat with AI", color = NeonPrimary) },
                                        onClick = {
                                            showMenu = false
                                            showAISummary = true
                                        },
                                        leadingIcon = { Icon(painter = painterResource(R.drawable.rotate_left_arrow_icon), contentDescription = null, tint = NeonPrimary, modifier = Modifier.size(24.dp)) }
                                    )
                                }
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
    ) { _ ->
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
            onShowInFolder = {},
            aiUiState = AIUiState.Idle,
            aiMessages = emptyList(),
            onSummarize = {},
            onAskAI = {}
        )
    }
}