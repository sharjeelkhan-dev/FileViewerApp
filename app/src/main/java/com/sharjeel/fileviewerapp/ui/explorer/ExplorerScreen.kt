package com.sharjeel.fileviewerapp.ui.explorer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.components.AppScaffold
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import com.sharjeel.fileviewerapp.ui.theme.NeonPrimary
import com.sharjeel.fileviewerapp.ui.theme.NeonSecondary
import com.sharjeel.fileviewerapp.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File
@Composable
fun ExplorerScreen(
    title: String,
    viewModel: ExplorerViewModel,
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    onPathClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val pickingArchive by viewModel.pickingFolderForArchive.collectAsState()
    val context = LocalContext.current
    val isMoving by viewModel.isMoving.collectAsState()
    val isCopying by viewModel.isCopying.collectAsState()

    var archiveToExtractTo by remember { mutableStateOf<FileModel?>(null) }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            archiveToExtractTo?.let { archive ->
                val path = com.sharjeel.fileviewerapp.util.FileUtils.getFolderPathFromUri(it)
                if (path != null) {
                    viewModel.extractArchive(context, archive.path, path)
                } else {
                    android.widget.Toast.makeText(context, "Could not resolve destination path. Please pick a location in Internal Storage.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        archiveToExtractTo = null
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ExplorerEvent.ShowMessage -> {
                    android.widget.Toast.makeText(context, event.message,
                        android.widget.Toast.LENGTH_SHORT).show()
                }
                is ExplorerEvent.NavigateToFolder -> {
                    viewModel.loadFiles(event.path)
                }
            }
        }
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<FileModel?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }

    if (fileToRename != null) {
        RenameDialog(
            fileName = fileToRename!!.name,
            onDismiss = { fileToRename = null },
            onConfirm = { newName ->
                viewModel.renameFile(fileToRename!!.path, newName)
                fileToRename = null
            }
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentType = sortType,
            currentOrder = sortOrder,
            onDismiss = { showSortSheet = false },
            onSortSelected = { type, order ->
                viewModel.setSort(type, order)
                showSortSheet = false
            }
        )
    }

    if (showViewOptionsSheet) {
        ViewOptionsBottomSheet(
            currentMode = viewMode,
            onDismiss = { showViewOptionsSheet = false },
            onModeSelected = { mode ->
                viewModel.setViewMode(mode)
                showViewOptionsSheet = false
            }
        )
    }

    BackHandler(enabled = selectedFiles.isNotEmpty() || isSearchActive || pickingArchive != null) {
        if (selectedFiles.isNotEmpty())
        {
            viewModel.clearSelection()
        } else if (isSearchActive) {
            isSearchActive = false
            viewModel.setSearchQuery("")
        } else if (pickingArchive != null) {
            viewModel.stopPickingFolder()
        }
    }
    AppScaffold(
        containerColor = MaterialTheme.colorScheme.background, // Themed background
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is ExplorerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonSecondary)
                    }
                }
                is ExplorerUiState.Success -> {
                    if (state.files.isEmpty() && !isSearchActive) {
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
                            viewMode = viewMode,
                            sortType = sortType,
                            sortOrder = sortOrder,
                            onSortClick = { showSortSheet = true },
                            onViewModeClick = { showViewOptionsSheet = true },
                            onBackClick = onBackClick,
                            onMenuClick = { showMenu = !showMenu },
                            showMenu = showMenu,
                            onDismissMenu = { showMenu = false },
                            isSearchActive = isSearchActive,
                            searchQuery = searchQuery,
                            onSearchToggle = { isSearchActive = it },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onRefreshClick = { viewModel.refresh() },
                            onSelectAllClick = { viewModel.selectAll() },
                            onDeleteSelectedClick = { viewModel.deleteSelectedFiles() },
                            onFileClick = { file ->
                                if (selectedFiles.isNotEmpty()) {
                                    viewModel.toggleFileSelection(file.path)
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onFileLongClick = { viewModel.toggleFileSelection(it.path) },
                            onDeleteClick = { viewModel.toggleFileSelection(it.path); viewModel.deleteSelectedFiles() },
                            onRenameClick = { fileToRename = it },
                            onShareClick = { FileUtils.shareFile(context, it.path) },
                            onOpenWithClick = { FileUtils.openWithExternalApp(context, it.path) },
                            onFavoriteClick = { viewModel.toggleFavorite(it) },
                            onExtractClick = { viewModel.extractArchive(context, it.path) },
                            onExtractToClick = { archiveToExtractTo = it; folderPickerLauncher.launch(null) },
                            onLockClick = { viewModel.moveToVault(it) },
                            onPathClick = { path ->
                                if (path == "CATEGORY_ROOT") {
                                    // Let MainScreen handle Home Screen navigation
                                    onPathClick(path)
                                } else {
                                    // Reload current category or navigate to physical path locally
                                    val rootPath = android.os.Environment.getExternalStorageDirectory().absolutePath
                                    if (path == rootPath) {
                                        // Tapping Root in category view should reload category
                                        when (title) {
                                            "Downloads" -> viewModel.loadCategory(com.sharjeel.fileviewerapp.domain.repository.FileCategory.DOWNLOADS)
                                            "Images" -> viewModel.loadCategory(com.sharjeel.fileviewerapp.domain.repository.FileCategory.IMAGES)
                                            "Videos" -> viewModel.loadCategory(com.sharjeel.fileviewerapp.domain.repository.FileCategory.VIDEOS)
                                            "Audio" -> viewModel.loadCategory(com.sharjeel.fileviewerapp.domain.repository.FileCategory.AUDIO)
                                            "Docs" -> viewModel.loadCategory(com.sharjeel.fileviewerapp.domain.repository.FileCategory.DOCUMENTS)
                                            "Archives" -> viewModel.loadCategory(com.sharjeel.fileviewerapp.domain.repository.FileCategory.ARCHIVES)
                                            "Recent" -> viewModel.loadRecent()
                                            "Favorites" -> viewModel.loadFavorites()
                                            else -> viewModel.loadFiles(rootPath)
                                        }
                                    } else {
                                        viewModel.loadFiles(path)
                                    }
                                }
                            },
                            onMoveClick = { viewModel.startMove(listOf(it.path)) },
                            onCopyClick = { viewModel.startCopy(listOf(it.path)) },
                            bottomPadding = 0.dp,
                            isMoving = isMoving,
                            isCopying = isCopying,
                            onPasteClick = { viewModel.paste() },
                            onCancelClick = { viewModel.cancelOperation() }
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
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
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
fun Breadcrumbs(currentPath: String, title: String, onPathClick: (String) -> Unit) {
    val isPreview = LocalInspectionMode.current
    val rootPath = remember {
        if (isPreview) "/storage/emulated/0"
        else android.os.Environment.getExternalStorageDirectory().absolutePath
    }

    val scrollState = rememberScrollState()

    // Determine the root label and whether we are in a physical path
    val isInsideInternalStorage = currentPath.startsWith(rootPath)
    
    // If title is not a physical path indicator, use it as the root label
    val isStorageTitle = title.equals("Storage", ignoreCase = true) || 
                         title.equals("Internal Storage", ignoreCase = true)
    
    val rootLabel = if (isStorageTitle || !isInsideInternalStorage) title else title
    
    // Actually, if we are in a category (like Archives), we want the category name as root.
    // When navigating into folders, we still want that category name as root.
    val effectiveRootLabel = if (isStorageTitle) "Internal Storage" else title.uppercase()

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp).offset(y = (-5).dp)
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Home Button
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (MaterialTheme.colorScheme.surface
                    .luminance() > 0.5f) Color(0xFFF3E5F5)
            else MaterialTheme.colorScheme.secondaryContainer,
            onClick = { onPathClick("CATEGORY_ROOT") } // Go back to Home Screen
        ) {
            Icon(
                painter = painterResource(id = R.drawable.house_window_icon),
                contentDescription = null,
                modifier = Modifier.padding(10.dp).size(20.dp),
                tint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f)
                    Color(0xFF7B1FA2) else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        // Dynamic Root Label
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f)
                Color(0xFFE0F7FA) else MaterialTheme.colorScheme.tertiaryContainer,
            onClick = { 
                if (isInsideInternalStorage && !isStorageTitle) {
                    onPathClick("CATEGORY_ROOT")
                } else {
                    onPathClick(rootPath)
                }
            }
        ) {
            Text(
                effectiveRootLabel,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f)
                    Color(0xFF00ACC1) else MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        // Path Segments (Only if inside Internal Storage)
        if (isInsideInternalStorage) {
            val relativePath = currentPath.removePrefix(rootPath).trimStart('/')
            val segments = if (relativePath.isEmpty()) emptyList() else relativePath.split('/')
            
            var cumulativePath = rootPath
            segments.forEach { segment ->
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                cumulativePath += "/$segment"
                val finalPath = cumulativePath
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    onClick = { onPathClick(finalPath) }
                ) {
                    Text(
                        segment,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SortBar(
    currentType: SortType,
    currentOrder: SortOrder,
    viewMode: ViewMode,
    onSortClick: () -> Unit,
    onViewModeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.clickable { onSortClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                currentType.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                if (currentOrder == SortOrder.ASCENDING)
                    Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = null, 
                modifier = Modifier.size(14.dp).padding(start = 4.dp),
                tint = NeonSecondary
            )
        }
        IconButton(onClick = onViewModeClick, modifier = Modifier.size(24.dp)) {
            Icon(
                when(viewMode) {
                    ViewMode.SMALL -> Icons.AutoMirrored.Rounded.ViewList
                    ViewMode.MEDIUM -> Icons.Rounded.GridView
                    ViewMode.LARGE -> Icons.Rounded.ViewModule
                }, 
                contentDescription = "Switch View", 
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun FileList(
    modifier: Modifier = Modifier,
    title: String,
    currentPath: String,
    files: List<FileModel>,
    selectedFiles: Set<String>,
    viewMode: ViewMode,
    sortType: SortType,
    sortOrder: SortOrder,
    onSortClick: () -> Unit,
    onViewModeClick: () -> Unit,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchToggle: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    onFileLongClick: (FileModel) -> Unit,
    onDeleteClick: (FileModel) -> Unit,
    onRenameClick: (FileModel) -> Unit,
    onShareClick: (FileModel) -> Unit,
    onOpenWithClick: (FileModel) -> Unit,
    onFavoriteClick: (FileModel) -> Unit,
    onExtractClick: (FileModel) -> Unit,
    onExtractToClick: (FileModel) -> Unit,
    onLockClick: (FileModel) -> Unit,
    onPathClick: (String) -> Unit,
    onMoveClick: (FileModel) -> Unit,
    onCopyClick: (FileModel) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    isMoving: List<String> = emptyList(),
    isCopying: List<String> = emptyList(),
    onPasteClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onRestoreSelectedClick: (() -> Unit)? = null,
    onRestoreAllClick: (() -> Unit)? = null,
    isEmptyTrashEnabled: Boolean = false,
    onEmptyTrashClick: (() -> Unit)? = null,
    showBreadcrumbs: Boolean = true
) {
    val columns = when (viewMode) {
        ViewMode.SMALL -> 1
        ViewMode.MEDIUM -> 3
        ViewMode.LARGE -> 2
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 0.dp, 
            bottom = bottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(columns) }) {
            Column {
                if (isSearchActive) {
                    SearchTopBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onCloseClick = { onSearchToggle(false); onSearchQueryChange("") }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack, 
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            title.uppercase(), 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (selectedFiles.isNotEmpty()) {
                            onRestoreSelectedClick?.let {
                                IconButton(onClick = it) {
                                    Icon(
                                        imageVector = Icons.Rounded.Restore,
                                        contentDescription = "Restore",
                                        tint = NeonSecondary
                                    )
                                }
                            }
                            IconButton(onClick = onDeleteSelectedClick) {
                                Icon(
                                    painter = painterResource(id = R.drawable.recycle_bin_line_icon),
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = { onSearchToggle(true) }) {
                            Icon(painter = painterResource(id = R.drawable.magnifying_glass_icon),
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Box {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Rounded.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onBackground)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = onDismissMenu,
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (isMoving.isNotEmpty() || isCopying.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Paste Here", fontWeight = FontWeight.Bold) },
                                        onClick = { 
                                            onDismissMenu()
                                            onPasteClick()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = NeonPrimary) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Cancel") },
                                        onClick = { 
                                            onDismissMenu()
                                            onCancelClick()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Cancel, contentDescription = null, tint = Color.Gray) }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                }

                                DropdownMenuItem(
                                    text = { Text("Refresh") },
                                    onClick = { 
                                        onDismissMenu()
                                        onRefreshClick()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = NeonPrimary) }
                                )
                                onRestoreAllClick?.let {
                                    DropdownMenuItem(
                                        text = { Text("Restore All") },
                                        onClick = { 
                                            onDismissMenu()
                                            it()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.RestorePage,
                                            contentDescription = null,
                                            tint = NeonSecondary) }
                                    )
                                }
                                if (isEmptyTrashEnabled) {
                                    DropdownMenuItem(
                                        text = { Text("Empty Trash") },
                                        onClick = { 
                                            onDismissMenu()
                                            onEmptyTrashClick?.invoke()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.DeleteForever,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Select All") },
                                    onClick = { 
                                        onDismissMenu()
                                        onSelectAllClick()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonPrimary) }
                                )
                            }
                        }
                    }
                }
                
                if (showBreadcrumbs) {
                    Breadcrumbs(currentPath, title, onPathClick)
                }
                
                SortBar(
                    currentType = sortType,
                    currentOrder = sortOrder,
                    viewMode = viewMode,
                    onSortClick = onSortClick,
                    onViewModeClick = onViewModeClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        items(files) { file ->
            if (viewMode == ViewMode.SMALL) {
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
                    onExtract = { onExtractClick(file) },
                    onExtractTo = { onExtractToClick(file) },
                    onLock = { onLockClick(file) },
                    onMove = { onMoveClick(file) },
                    onCopy = { onCopyClick(file) },
                    onPathClick = onPathClick
                )
            } else {
                FileGridItem(
                    file = file,
                    isSelected = selectedFiles.contains(file.path),
                    onClick = { onFileClick(file) },
                    onLongClick = { onFileLongClick(file) },
                    onDelete = { onDeleteClick(file) },
                    onRename = { onRenameClick(file) },
                    onShare = { onShareClick(file) },
                    onOpenWith = { onOpenWithClick(file) },
                    onFavorite = { onFavoriteClick(file) },
                    onExtract = { onExtractClick(file) },
                    onExtractTo = { onExtractToClick(file) },
                    onLock = { onLockClick(file) },
                    onMove = { onMoveClick(file) },
                    onCopy = { onCopyClick(file) },
                    onPathClick = onPathClick
                )
            }
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
    onExtract: () -> Unit,
    onExtractTo: () -> Unit,
    onLock: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onPathClick: (String) -> Unit)
{
    Surface(
        color = if (isSelected) NeonSecondary.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) androidx.compose.foundation
            .BorderStroke(2.dp, NeonSecondary) else null,
        shadowElevation = if (isSelected) 0.dp else 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp).offset(y = (-15).dp)
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
                Icon(Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = NeonSecondary)
            } else {
                var showFileMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showFileMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert, 
                            contentDescription = "Actions", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    DropdownMenu(
                        expanded = showFileMenu,
                        onDismissRequest = { showFileMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.widthIn(min = 200.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onShare()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(painter = painterResource(id = R.drawable.share_icon),
                                contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Open with", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onOpenWith()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(painter = painterResource(id = R.drawable.shortcut_icon),
                                contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Show in Folder", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onPathClick(File(file.path).parent ?: "")
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(painter = painterResource(id = R.drawable.open_folder_outline_icon),
                                contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Favorites", fontWeight = FontWeight.SemiBold) },
                            onClick = { 
                                onFavorite()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Rounded.StarBorder,
                                contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) }
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        DropdownMenuItem(
                            text = { Text("Rename", fontWeight = FontWeight.Medium) },
                            onClick = { 
                                onRename()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(painterResource(R.drawable.brush_paintbrush_icon),
                                contentDescription = null, tint = NeonPrimary, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Move", fontWeight = FontWeight.Medium) },
                            onClick = { 
                                onMove()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Rounded.MoveUp,
                                contentDescription = null, tint = NeonPrimary, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy", fontWeight = FontWeight.Medium) },
                            onClick = { 
                                onCopy()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(painterResource(R.drawable.copy_outline_icon),
                                contentDescription = null, tint = NeonPrimary, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", fontWeight = FontWeight.Medium) },
                            onClick = { 
                                onDelete()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(painterResource(R.drawable.recycle_bin_line_icon),
                                contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Lock (Vault)", fontWeight = FontWeight.Medium) },
                            onClick = { 
                                onLock()
                                showFileMenu = false 
                            },
                            leadingIcon = { Icon(painterResource(R.drawable.lock_line_icon),
                                contentDescription = null, tint = NeonSecondary, modifier = Modifier.size(20.dp)) }
                        )
                        if (file.extension.lowercase() in listOf("zip", "rar")) {
                            DropdownMenuItem(
                                text = { Text("Extract Here", fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    onExtract()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painterResource(R.drawable.check_mark_circle_line_icon),
                                    contentDescription = null, tint = NeonPrimary, modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Extract to...", fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    onExtractTo()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painterResource(R.drawable.open_folder_outline_icon),
                                    contentDescription = null, tint = NeonPrimary, modifier = Modifier.size(20.dp)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FileModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onFavorite: () -> Unit,
    onExtract: () -> Unit,
    onExtractTo: () -> Unit,
    onLock: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onPathClick: (String) -> Unit
) {
    Surface(
        color = if (isSelected) NeonSecondary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) androidx.compose.foundation
            .BorderStroke(2.dp, NeonSecondary) else null,
        shadowElevation = if (isSelected) 0.dp else 2.dp,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .aspectRatio(0.9f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.align(Alignment.Center).size(64.dp),
                    contentAlignment = Alignment.Center) {
                    FileThumbnail(file)
                    if (isSelected) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f),
                                RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = NeonSecondary)
                        }
                    }
                }
                
                if (!isSelected) {
                    var showFileMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(
                            onClick = { showFileMenu = true },
                            modifier = Modifier.size(24.dp).offset(x = 8.dp, y = (-8).dp)
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert, 
                                contentDescription = "Actions", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showFileMenu,
                            onDismissRequest = { showFileMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.widthIn(min = 200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share", fontWeight = FontWeight.SemiBold) },
                                onClick = { 
                                    onShare()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painter = painterResource(id = R.drawable.share_icon),
                                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Open with", fontWeight = FontWeight.SemiBold) },
                                onClick = { 
                                    onOpenWith()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painter = painterResource(id = R.drawable.shortcut_icon),
                                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Show in Folder", fontWeight = FontWeight.SemiBold) },
                                onClick = { 
                                    onPathClick(File(file.path).parent ?: "")
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painter = painterResource(id = R.drawable.open_folder_outline_icon),
                                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Favorites", fontWeight = FontWeight.SemiBold) },
                                onClick = { 
                                    onFavorite()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Rounded.StarBorder,
                                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)) }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), 
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            DropdownMenuItem(
                                text = { Text("Rename", fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    onRename()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painter = painterResource(id = R.drawable.brush_paintbrush_icon),
                                    contentDescription = null, tint = NeonPrimary,
                                    modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Move", fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    onMove()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Rounded.MoveUp,
                                    contentDescription = null, tint = NeonPrimary,
                                    modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy", fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    onCopy()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painterResource(R.drawable.copy_outline_icon),
                                    contentDescription = null, tint = NeonPrimary,
                                    modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    onDelete()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painterResource(R.drawable.recycle_bin_line_icon),
                                    contentDescription = null, tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Lock (Vault)", fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    onLock()
                                    showFileMenu = false 
                                },
                                leadingIcon = { Icon(painterResource(R.drawable.lock_line_icon),
                                    contentDescription = null, tint = NeonPrimary,
                                    modifier = Modifier.size(20.dp)) }
                            )
                            if (file.extension.lowercase() in listOf("zip", "rar")) {
                                DropdownMenuItem(
                                    text = { Text("Extract Here", fontWeight = FontWeight.Medium) },
                                    onClick = { 
                                        onExtract()
                                        showFileMenu = false 
                                    },
                                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.check_mark_circle_line_icon),
                                        contentDescription = null, tint = NeonPrimary,
                                        modifier = Modifier.size(20.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Extract to...", fontWeight = FontWeight.Medium) },
                                    onClick = { 
                                        onExtractTo()
                                        showFileMenu = false 
                                    },
                                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.open_folder_outline_icon),
                                        contentDescription = null, tint = NeonPrimary,
                                        modifier = Modifier.size(20.dp)) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (file.isDirectory) "${file.itemCount} Items" else FileUtils.formatFileSize(file.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RenameDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(fileName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File", fontWeight = FontWeight.Bold) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPrimary)
            ) {
                Text("Rename", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
@Composable
fun FileThumbnail(file: FileModel) {
    val isVideo = FileUtils.isVideoFile(file.path)
    val isImage = FileUtils.isImageFile(file.path)
    val isApk = file.extension.lowercase() == "apk"
    val isPdf = file.extension.lowercase() == "pdf"
    
    if (isVideo || isImage || isApk || isPdf) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (isPdf) {
                PdfThumbnail(file.path, Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = file.path, 
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(R.drawable.photo_collage_icon),
                    error = painterResource(if (isApk) R.drawable.archive_line_icon else R.drawable.photo_collage_icon)
                )
            }
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
    }
    else
    {
        val (icon, color) = when
        {
            file.isDirectory -> painterResource(R.drawable.archive_line_icon) to Color(0xFF64B5F6)
            file.extension.lowercase() in listOf("doc", "docx") -> painterResource(R.drawable.text_document_line_icon) to Color(0xFF1E88E5)
            file.extension.lowercase() in listOf("xls", "xlsx") -> painterResource(R.drawable.text_document_line_icon) to Color(0xFF43A047)
            file.extension.lowercase() in listOf("ppt", "pptx") -> painterResource(R.drawable.text_document_line_icon) to Color(0xFFF4511E)
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

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@Composable
fun PdfThumbnail(path: String, modifier: Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) return@withContext null
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    // Render at thumbnail size for performance
                    val targetWidth = 150 
                    val targetHeight = (page.height.toFloat() / page.width.toFloat() * targetWidth).toInt()
                    val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    pfd.close()
                    bmp
                } else {
                    renderer.close()
                    pfd.close()
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFFEF5350).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.text_document_line_icon),
                contentDescription = null,
                tint = Color(0xFFEF5350),
                modifier = Modifier.size(24.dp)
            )
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
        )
        { padding ->
            Box(modifier = Modifier.padding(top = padding.calculateTopPadding()))
            {
                FileList(
                    title = "Downloads",
                    currentPath = "/storage/emulated/0/Download",
                    files = mockFiles,
                    selectedFiles = setOf("/path/img.jpg"),
                    viewMode = ViewMode.SMALL,
                    sortType = SortType.NAME,
                    sortOrder = SortOrder.ASCENDING,
                    onSortClick = {},
                    onViewModeClick = {},
                    onBackClick = {},
                    onMenuClick = {},
                    showMenu = false,
                    onDismissMenu = {},
                    isSearchActive = false,
                    searchQuery = "",
                    onSearchToggle = {},
                    onSearchQueryChange = {},
                    onRefreshClick = {},
                    onSelectAllClick = {},
                    onDeleteSelectedClick = {},
                    onFileClick = {},
                    onFileLongClick = {},
                    onDeleteClick = {},
                    onRenameClick = {},
                    onShareClick = {},
                    onOpenWithClick = {},
                    onFavoriteClick = {},
                    onExtractClick = {},
                    onExtractToClick = {},
                    onLockClick = {},
                    onPathClick = {},
                    onMoveClick = {},
                    onCopyClick = {},
                    bottomPadding = 0.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentType: SortType,
    currentOrder: SortOrder,
    onDismiss: () -> Unit,
    onSortSelected: (SortType, SortOrder) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Sort",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("Name", "Type", "Size", "Date").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ascending Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SortType.entries.forEach { type ->
                    SortOptionBox(
                        type = type,
                        order = SortOrder.ASCENDING,
                        isSelected = currentType == type && currentOrder == SortOrder.ASCENDING,
                        onClick = { onSortSelected(type, SortOrder.ASCENDING) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Descending Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SortType.entries.forEach { type ->
                    SortOptionBox(
                        type = type,
                        order = SortOrder.DESCENDING,
                        isSelected = currentType == type && currentOrder == SortOrder.DESCENDING,
                        onClick = { onSortSelected(type, SortOrder.DESCENDING) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SortOptionBox(
    type: SortType,
    order: SortOrder,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        SortType.NAME -> Icons.Rounded.SortByAlpha
        SortType.TYPE -> Icons.AutoMirrored.Rounded.InsertDriveFile
        SortType.SIZE -> Icons.Rounded.PieChart
        SortType.DATE -> Icons.Rounded.Schedule
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Icon(
                imageVector = if (order == SortOrder.ASCENDING)
                    Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOptionsBottomSheet(
    currentMode: ViewMode,
    onDismiss: () -> Unit,
    onModeSelected: (ViewMode) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "View Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ViewMode.entries.forEach { mode ->
                    ViewModeBox(
                        mode = mode,
                        isSelected = currentMode == mode,
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ViewModeBox(
    mode: ViewMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (mode) {
        ViewMode.SMALL -> Icons.AutoMirrored.Rounded.ViewList
        ViewMode.MEDIUM -> Icons.Rounded.GridView
        ViewMode.LARGE -> Icons.Rounded.ViewModule
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) NeonSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) NeonSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) NeonSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                Breadcrumbs("Downloads", "Downloads", onPathClick = {})
                SortBar(
                    currentType = SortType.NAME,
                    currentOrder = SortOrder.ASCENDING,
                    viewMode = ViewMode.SMALL,
                    onSortClick = {},
                    onViewModeClick = {}
                )
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
                        onExtract = {},
                        onExtractTo = {},
                        onLock = {},
                        onMove = {},
                        onCopy = {},
                        onPathClick = {}
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
                        onExtract = {},
                        onExtractTo = {},
                        onLock = {},
                        onMove = {},
                        onCopy = {},
                        onPathClick = {}
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
                Breadcrumbs("Camera", "Camera", onPathClick = {})
                SortBar(
                    currentType = SortType.NAME,
                    currentOrder = SortOrder.ASCENDING,
                    viewMode = ViewMode.SMALL,
                    onSortClick = {},
                    onViewModeClick = {}
                )
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
                        onExtract = {},
                        onExtractTo = {},
                        onLock = {},
                        onMove = {},
                        onCopy = {},
                        onPathClick = {}
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
                        onExtract = {},
                        onExtractTo = {},
                        onLock = {},
                        onMove = {},
                        onCopy = {},
                        onPathClick = {}
                    )
                }
            }
        }
    }
}
