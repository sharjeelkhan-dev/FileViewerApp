package com.sharjeel.fileviewerapp.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MoveUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import com.sharjeel.fileviewerapp.ui.theme.NeonPrimary
import com.sharjeel.fileviewerapp.ui.theme.NeonSecondary
import com.sharjeel.fileviewerapp.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    title: String,
    viewModel: ExplorerViewModel,
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val context = LocalContext.current

    var isSearchActive by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedFiles.isNotEmpty() || isSearchActive) {
        if (selectedFiles.isNotEmpty()) {
            viewModel.clearSelection()
        } else if (isSearchActive) {
            isSearchActive = false
            viewModel.setSearchQuery("")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                if (isSearchActive) {
                    SearchTopBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onCloseClick = { 
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                        }
                    )
                } else {
                    TopAppBar(
                        title = { 
                            Text(
                                title.uppercase(), 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (selectedFiles.isNotEmpty()) {
                                    viewModel.clearSelection()
                                } else {
                                    onBackClick()
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack, 
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            if (selectedFiles.isNotEmpty()) {
                                IconButton(onClick = { viewModel.deleteSelectedFiles() }) {
                                    Icon(Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(painter = painterResource(id = R.drawable.magnifying_glass_icon),
                                    contentDescription = "Search",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onBackground)
                            }
                            Box {
                                IconButton(onClick = { showMenu = !showMenu }) {
                                    Icon(Icons.Rounded.MoreVert,
                                        contentDescription = "More",
                                        tint = MaterialTheme.colorScheme.onBackground)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    containerColor = MaterialTheme
                                        .colorScheme.surface.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        onClick = { 
                                            showMenu = false 
                                            viewModel.refresh()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            tint = NeonPrimary) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Select All") },
                                        onClick = { 
                                            showMenu = false 
                                            // Select All logic here
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = NeonPrimary) }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        windowInsets = TopAppBarDefaults.windowInsets
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when (val state = uiState) {
                is ExplorerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonSecondary)
                    }
                }
                is ExplorerUiState.Success -> {
                    if (state.files.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.FolderOpen, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(64.dp), 
                                    tint = MaterialTheme.colorScheme
                                        .onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No files found", color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant)
                            }
                        }
                    } else {
                        FileList(
                            title = title,
                            currentPath = currentPath,
                            files = state.files,
                            selectedFiles = selectedFiles,
                            onFileClick = { file ->
                                if (selectedFiles.isNotEmpty()) {
                                    viewModel.toggleFileSelection(file.path)
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onFileLongClick = { viewModel.toggleFileSelection(it.path) },
                            onDeleteClick = { viewModel.toggleFileSelection(it.path); viewModel.deleteSelectedFiles() },
                            onRenameClick = { /* Implement Rename Dialog */ },
                            onShareClick = { FileUtils.openWithExternalApp(context, it.path) },
                            onOpenWithClick = { FileUtils.openWithExternalApp(context, it.path) },
                            onFavoriteClick = { /* viewModel.toggleFavorite(it) */ },
                            onExtractClick = { viewModel.extractArchive(it.path) },
                            onPathClick = { viewModel.loadFiles(it) },
                            bottomPadding = innerPadding.calculateBottomPadding()
                        )
                    }
                }
                is ExplorerUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseClick: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search files...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Close Search")
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Clear,
                        contentDescription = "Clear")
                }
            }
        },
        windowInsets = TopAppBarDefaults.windowInsets
    )
}

@Composable
fun Breadcrumbs(currentPath: String, onPathClick: (String) -> Unit) {
    val rootPath = android.os.Environment.getExternalStorageDirectory().absolutePath
    val relativePath = currentPath.removePrefix(rootPath).trimStart('/')
    val segments = if (relativePath.isEmpty()) emptyList() else relativePath.split('/')
    
    val scrollState = androidx.compose.foundation.rememberScrollState()
    
    Row(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home Button
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NeonPrimary.copy(alpha = 0.1f),
            onClick = { onPathClick(rootPath) }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.house_window_icon),
                contentDescription = null, 
                modifier = Modifier.padding(6.dp).size(18.dp),
                tint = NeonPrimary
            )
        }

        // Internal Storage Label (always present after home if we are in internal storage)
        BreadcrumbSegment("Internal Storage", rootPath, onPathClick)

        // Path Segments
        var cumulativePath = rootPath
        segments.forEach { segment ->
            cumulativePath += "/$segment"
            BreadcrumbSegment(segment, cumulativePath, onPathClick)
        }
    }
}

@Composable
private fun BreadcrumbSegment(name: String, path: String, onClick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.ChevronRight, 
            contentDescription = null, 
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            onClick = { onClick(path) }
        ) {
            Text(
                name,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NeonSecondary
            )
        }
    }
}

@Composable
fun SortBar() {
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "NAME",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Rounded.ArrowUpward, 
                contentDescription = null, 
                modifier = Modifier.size(14.dp).padding(start = 4.dp),
                tint = NeonSecondary
            )
        }
        Icon(
            Icons.Rounded.GridView, 
            contentDescription = "Switch View", 
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FileList(
    title: String,
    currentPath: String,
    files: List<FileModel>,
    selectedFiles: Set<String>,
    onFileClick: (FileModel) -> Unit,
    onFileLongClick: (FileModel) -> Unit,
    onDeleteClick: (FileModel) -> Unit,
    onRenameClick: (FileModel) -> Unit,
    onShareClick: (FileModel) -> Unit,
    onOpenWithClick: (FileModel) -> Unit,
    onFavoriteClick: (FileModel) -> Unit,
    onExtractClick: (FileModel) -> Unit,
    onPathClick: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp,
            end = 16.dp, top = 0.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                Breadcrumbs(currentPath, onPathClick)
                SortBar()
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        items(files) { file ->
            FileItem(
                file = file,
                isSelected = selectedFiles.contains(file.path),
                onClick = { onFileClick(file) },
                onLongClick = { onFileLongClick(file) },
                onDelete = { onDeleteClick(file) },
                onRename = { onRenameClick(file) },
                onShare = { onShareClick(file) },
                onOpenWith = { onOpenWithClick(file) },
                onFavorite = { onFavoriteClick(file) },
                onExtract = { onExtractClick(file) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    file: FileModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onFavorite: () -> Unit,
    onExtract: () -> Unit
) {
    Surface(
        color = if (isSelected) NeonSecondary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, NeonSecondary) else null,
        shadowElevation = if (isSelected) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileThumbnail(file)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (file.isDirectory) {
                    Text(
                        text = "${file.itemCount} Items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "${FileUtils.formatFileSize(file.size)} • ${FileUtils.formatDate(file.lastModified)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NeonSecondary)
            } else {
                var showFileMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showFileMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert, 
                            contentDescription = "Actions", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    DropdownMenu(
                        expanded = showFileMenu,
                        onDismissRequest = { showFileMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("File Info", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                showFileMenu = false
                            },
                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = NeonPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onRename()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = NeonPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Move", fontWeight = FontWeight.SemiBold) },
                            onClick = { showFileMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.MoveUp, contentDescription = null, tint = NeonPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy", fontWeight = FontWeight.SemiBold) },
                            onClick = { showFileMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = NeonPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onDelete()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF5350)) }
                        )
                        if (file.extension.lowercase() in listOf("zip", "rar")) {
                            DropdownMenuItem(
                                text = { Text("Extract Here", fontWeight = FontWeight.SemiBold) },
                                onClick = { 
                                    onExtract()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painterResource(R.drawable.archive_line_icon), contentDescription = null, tint = NeonPrimary) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Select", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onLongClick()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NeonSecondary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onShare()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = NeonSecondary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Open with", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onOpenWith()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = NeonSecondary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Favorites", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onFavorite()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFD600)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileThumbnail(file: FileModel) {
    val isVideo = FileUtils.isVideoFile(file.path)
    val isImage = FileUtils.isImageFile(file.path)
    
    if (isVideo || isImage) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            AsyncImage(
                model = file.path, 
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(R.drawable.photo_collage_icon),
                error = painterResource(R.drawable.photo_collage_icon)
            )
            if (isVideo) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.video_playlist_icon),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
        val (icon, color) = when {
            file.isDirectory -> painterResource(R.drawable.archive_line_icon) to Color(0xFF64B5F6)
            file.extension.lowercase() == "pdf" -> painterResource(R.drawable.text_document_line_icon) to Color(0xFFEF5350)
            file.extension.lowercase() in listOf("mp3", "wav", "flac", "opus", "ogg") -> painterResource(R.drawable.audio_tune_icon) to Color(0xFF66BB6A)
            file.extension.lowercase() in listOf("zip", "rar", "7z") -> painterResource(R.drawable.archive_line_icon) to Color(0xFF78909C)
            else -> painterResource(R.drawable.text_document_line_icon) to Color(0xFFBDBDBD)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.15f),
                    RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Explorer Preview - Full Screen")
@Composable
fun ExplorerPreviewFull() {
    FileViewerAppTheme(darkTheme = true) {
        val mockFiles = listOf(
            FileModel("Documents", "/path/docs", 0L, System.currentTimeMillis(), true, itemCount = 5),
            FileModel("Vacation.jpg", "/path/img.jpg", 2 * 1024 * 1024L, System.currentTimeMillis(), false, extension = "jpg"),
            FileModel("Contract.pdf", "/path/doc.pdf", 500 * 1024L, System.currentTimeMillis(), false, extension = "pdf"),
            FileModel("Video_Recording.mp4", "/path/vid.mp4", 45 * 1024 * 1024L, System.currentTimeMillis(), false, extension = "mp4")
        )
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column {
                        TopAppBar(
                            title = { Text("DOWNLOADS", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onBackground) },
                            navigationIcon = {
                                IconButton(onClick = {}) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack, 
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            },
                            actions = {
                                Icon(painter = painterResource(id = R.drawable.magnifying_glass_icon),
                                    contentDescription = null,
                                    modifier = Modifier.padding(12.dp).size(20.dp))
                                Icon(Icons.Rounded.MoreVert,
                                    contentDescription = null,
                                    modifier = Modifier.padding(12.dp))
                            }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
                FileList(
                    title = "Downloads",
                    currentPath = "/storage/emulated/0/Download",
                    files = mockFiles,
                    selectedFiles = setOf("/path/img.jpg"),
                    onFileClick = {},
                    onFileLongClick = {},
                    onDeleteClick = {},
                    onRenameClick = {},
                    onShareClick = {},
                    onOpenWithClick = {},
                    onFavoriteClick = {},
                    onExtractClick = {},
                    onPathClick = {},
                    bottomPadding = 0.dp
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun ExplorerPreviewLight() {
    FileViewerAppTheme(darkTheme = false) {
        Box(modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background))
        {
            Column {
                Breadcrumbs("Downloads", onPathClick = {})
                SortBar()
                Column(modifier = Modifier.padding(16.dp)) {
                    FileItem(
                        file = FileModel("Documents", "/path/docs"
                            , 0L, System.currentTimeMillis(),
                            true, itemCount = 5),
                        isSelected = false,
                        onClick = {},
                        onLongClick = {},
                        onDelete = {},
                        onRename = {},
                        onShare = {},
                        onOpenWith = {},
                        onFavorite = {},
                        onExtract = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FileItem(
                        file = FileModel("Vacation.jpg", "/path/img.jpg",
                            2 * 1024 * 1024L, System.currentTimeMillis(),
                            false,
                            extension = "jpg"),
                        isSelected = true,
                        onClick = {},
                        onLongClick = {},
                        onDelete = {},
                        onRename = {},
                        onShare = {},
                        onOpenWith = {},
                        onFavorite = {},
                        onExtract = {}
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Dark Mode - List")
@Composable
fun ExplorerPreviewDark() {
    FileViewerAppTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            Column {
                Breadcrumbs("Camera", onPathClick = {})
                SortBar()
                Column(modifier = Modifier.padding(16.dp)) {
                    FileItem(
                        file = FileModel("Videos", "/path/videos",
                            0L, System.currentTimeMillis(),
                            true, itemCount = 12),
                        isSelected = false,
                        onClick = {},
                        onLongClick = {},
                        onDelete = {},
                        onRename = {},
                        onShare = {},
                        onOpenWith = {},
                        onFavorite = {},
                        onExtract = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FileItem(
                        file = FileModel("Document.pdf", "/path/doc.pdf",
                            500 * 1024L, System.currentTimeMillis(),
                            false, extension = "pdf"),
                        isSelected = false,
                        onClick = {},
                        onLongClick = {},
                        onDelete = {},
                        onRename = {},
                        onShare = {},
                        onOpenWith = {},
                        onFavorite = {},
                        onExtract = {}
                    )
                }
            }
        }
    }
}
