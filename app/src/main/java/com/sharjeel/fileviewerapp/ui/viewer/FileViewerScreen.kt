package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.ai.AIUiState
import com.sharjeel.fileviewerapp.ui.ai.AIViewModel
import com.sharjeel.fileviewerapp.ui.components.AppScaffold
import java.io.File

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

    val currentFile = if ((currentFileIndex != -1) && (currentFileIndex < filePlaylist.size)) {
        filePlaylist[currentFileIndex]
    } else null

    val effectiveFilePath = currentFile?.path ?: filePath
    val fileModel = remember(effectiveFilePath) { FileModel.fromFile(File(effectiveFilePath)) }

    LaunchedEffect(effectiveFilePath) {
        if (filePlaylist.isEmpty()) {
            viewModel.loadFolderPlaylist(filePath)
        }
        viewModel.checkIfFavorite(effectiveFilePath)
        viewModel.addToRecent(fileModel)
        
        // 🎯 FIXED: Reset AI context when swiping to a different file
        aiViewModel.clearChat()
        aiViewModel.resetState()
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
    val isText = effectiveFileType in listOf("txt", "log", "json", "xml", "kt", "java", "csv")
    val isOffice = effectiveFileType in listOf("docx", "doc", "xlsx", "xls", "pptx", "ppt", "epub")
    
    val isMedia = isAudio || isVideo || isImage
    val isAnyDocument = isPdf || isText || isOffice

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
                    color = MaterialTheme.colorScheme.primary
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
                        when (aiUiState) {
                            is AIUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            is AIUiState.SummaryReady -> {
                                // 🎯 FIXED: Wrapped AI Summary in SelectionContainer
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(aiUiState.summary, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            is AIUiState.Error -> Text(aiUiState.message, color = MaterialTheme.colorScheme.error)
                            else -> Text("Generating summary...")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(aiMessages) { msg ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(
                                color = if (msg.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                // 🎯 FIXED: Wrapped Chat Messages in SelectionContainer
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        msg.content,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = userQuestion,
                    onValueChange = { userQuestion = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (isAudio || isVideo || isImage) "Ask about this media..." else "Ask about this document...") },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (userQuestion.isNotBlank()) {
                                onAskAI(userQuestion)
                                userQuestion = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }

    // 🎯 Ab kisi bhi category ke liye koi custom black/white conditions nahi hain.
    // Sab kuch application ke main central theme colors se map ho raha hai.
    val scaffoldContainerColor = MaterialTheme.colorScheme.background
    val topAppBarContainerColor = MaterialTheme.colorScheme.surface
    val topAppBarContentColor = MaterialTheme.colorScheme.onSurface

    AppScaffold(
        containerColor = scaffoldContainerColor,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scaffoldContainerColor)
        ) {
            // Main Content - Full Screen (Ignoring top bar padding to prevent jumping)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (filePlaylist.isNotEmpty()) {
                    UniversalFilePager(
                        filePlaylist = filePlaylist,
                        initialIndex = filePlaylist.indexOfFirst { it.path == effectiveFilePath }.coerceAtLeast(0),
                        controlsVisible = controlsVisible,
                        onToggleControls = { controlsVisible = !controlsVisible },
                        onFileChanged = { /* Centralized handle check block */ },
                        onIndexChanged = onUpdateFileIndex
                    )
                } else {
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

            // Top Bar Overlay - Prevents "jumping" content when hidden/shown
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            effectiveFileName,
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
                                    modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }, modifier = if (isLandscape) Modifier.size(40.dp) else Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More"
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

                                // 🎯 FIXED: Added Copy Text functionality for documents
                                if (isPdf || effectiveFileType in listOf("txt", "docx", "doc", "json", "xml", "kt", "java", "log", "csv")) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    DropdownMenuItem(
                                        text = { Text("Copy All Text") },
                                        onClick = {
                                            showMenu = false
                                            val text = com.sharjeel.fileviewerapp.util.TextExtractionUtils.extractText(effectiveFilePath)
                                            if (!text.isNullOrBlank()) {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("File Text", text)
                                                clipboard.setPrimaryClip(clip)
                                                android.widget.Toast.makeText(context, "Text copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "No text found to copy", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) }
                                    )
                                }

                                if (isPdf || isText || isAudio || isVideo || isImage) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    DropdownMenuItem(
                                        text = { Text("AI Summarize", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showMenu = false
                                            onSummarize()
                                            showAISummary = true
                                        },
                                        leadingIcon = { Icon(painterResource(R.drawable.brush_paintbrush_icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Chat with AI", color = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showMenu = false
                                            showAISummary = true
                                        },
                                        leadingIcon = { Icon(painter = painterResource(R.drawable.rotate_left_arrow_icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topAppBarContainerColor.copy(alpha = 0.9f),
                        titleContentColor = topAppBarContentColor,
                        navigationIconContentColor = topAppBarContentColor,
                        actionIconContentColor = topAppBarContentColor
                    ),
                    windowInsets = if (isLandscape) WindowInsets(0, 0, 0, 0) else TopAppBarDefaults.windowInsets
                )
            }
        }
    }
}
