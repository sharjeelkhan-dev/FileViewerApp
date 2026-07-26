package com.sharjeel.fileviewerapp.ui.explorer

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.util.FileUtils

@Composable
fun ExplorerScreen(
    title: String,
    viewModel: ExplorerViewModel,
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    onPathClick: (String) -> Unit,
    onHomeClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val breadcrumbsList by viewModel.breadcrumbs.collectAsState()
    val isMoving by viewModel.isMoving.collectAsState()
    val isCopying by viewModel.isCopying.collectAsState()
    val pickingFolderForArchive by viewModel.pickingFolderForArchive.collectAsState()

    val localContext = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExplorerEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is ExplorerEvent.NavigateToFolder -> onPathClick(event.path)
                ExplorerEvent.NavigateToHome -> onHomeClick()
            }
        }
    }

    ExplorerScreenContent(
        title = title,
        uiState = uiState,
        selectedFiles = selectedFiles,
        searchQuery = searchQuery,
        sortType = sortType,
        sortOrder = sortOrder,
        viewMode = viewMode,
        breadcrumbsList = breadcrumbsList,
        isMoving = isMoving,
        isCopying = isCopying,
        pickingFolderForArchive = pickingFolderForArchive,
        onBackClick = onBackClick,
        onFileClick = onFileClick,
        onRenameFile = { path, newName -> viewModel.renameFile(path, newName) },
        onSetSort = { type, order -> viewModel.setSort(type, order) },
        onSetViewMode = { mode -> viewModel.setViewMode(mode) },
        onStopPickingFolder = { viewModel.stopPickingFolder() },
        onExtractToCurrentFolder = { viewModel.extractToCurrentFolder() },
        onClearSelection = { viewModel.clearSelection() },
        onSetSearchQuery = { viewModel.setSearchQuery(it) },
        onDeleteSelectedFiles = { viewModel.deleteSelectedFiles() },
        onStartMove = { viewModel.startMove(it) },
        onStartCopy = { viewModel.startCopy(it) },
        onPaste = { viewModel.paste() },
        onCancelOperation = { viewModel.cancelOperation() },
        onRefresh = { viewModel.refresh() },
        onSelectAllPaths = { viewModel.selectAllPaths(it) },
        onToggleFileSelection = { viewModel.toggleFileSelection(it) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onExtractArchive = { viewModel.extractArchive(localContext, it) },
        onMoveToVault = { viewModel.moveToVault(it) },
        onBreadcrumbClick = { item ->
            if (item.category != null) {
                viewModel.loadCategory(item.category)
            } else if (item.path.isNotEmpty()) {
                viewModel.loadFiles(item.path)
                onPathClick(item.path)
            } else {
                viewModel.resetToHome()
                onHomeClick()
            }
        },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreenContent(
    title: String,
    uiState: ExplorerUiState,
    selectedFiles: Set<String>,
    searchQuery: String,
    sortType: SortType,
    sortOrder: SortOrder,
    viewMode: ViewMode,
    breadcrumbsList: List<BreadcrumbItem>,
    isMoving: List<String>,
    isCopying: List<String>,
    pickingFolderForArchive: FileModel?,
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    onRenameFile: (String, String) -> Unit,
    onSetSort: (SortType, SortOrder) -> Unit,
    onSetViewMode: (ViewMode) -> Unit,
    onStopPickingFolder: () -> Unit,
    onExtractToCurrentFolder: () -> Unit,
    onClearSelection: () -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onDeleteSelectedFiles: () -> Unit,
    onStartMove: (List<String>) -> Unit,
    onStartCopy: (List<String>) -> Unit,
    onPaste: () -> Unit,
    onCancelOperation: () -> Unit,
    onRefresh: () -> Unit,
    onSelectAllPaths: (List<String>) -> Unit,
    onToggleFileSelection: (String) -> Unit,
    onToggleFavorite: (FileModel) -> Unit,
    onExtractArchive: (String) -> Unit,
    onMoveToVault: (FileModel) -> Unit,
    onBreadcrumbClick: (BreadcrumbItem) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<FileModel?>(null) }
    var fileForActions by remember { mutableStateOf<FileModel?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }
    val localContext = LocalContext.current

    val isSelectionActive = selectedFiles.isNotEmpty()

    if (fileToRename != null) {
        RenameDialog(
            fileName = fileToRename!!.name,
            onDismiss = { fileToRename = null },
            onConfirm = { newName ->
                onRenameFile(fileToRename!!.path, newName)
                fileToRename = null
            }
        )
    }

    if (fileForActions != null) {
        FileActionBottomSheet(
            file = fileForActions!!,
            onDismiss = { fileForActions = null },
            onRenameClick = { onRenameFile(it.path, fileToRename?.name ?: it.name); fileToRename = it },
            onMoveClick = { onStartMove(listOf(it.path)) },
            onCopyClick = { onStartCopy(listOf(it.path)) },
            onDeleteClick = { onToggleFileSelection(it.path); onDeleteSelectedFiles() },
            onExtractClick = { onExtractArchive(it.path) },
            onFavoriteClick = { onToggleFavorite(it) },
            onLockClick = { onMoveToVault(it) },
            onShareClick = { FileUtils.shareFile(localContext, it.path) },
            onOpenWithClick = { FileUtils.openWithExternalApp(localContext, it.path) },
            onSelectClick = { onToggleFileSelection(it.path) }
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentType = sortType,
            currentOrder = sortOrder,
            onDismiss = { showSortSheet = false },
            onSortSelected = { type, order ->
                onSetSort(type, order)
                showSortSheet = false
            }
        )
    }

    if (showViewOptionsSheet) {
        ViewOptionsBottomSheet(
            currentMode = viewMode,
            onDismiss = { showViewOptionsSheet = false },
            onModeSelected = { mode ->
                onSetViewMode(mode)
                showViewOptionsSheet = false
            }
        )
    }

    if (pickingFolderForArchive != null) {
        AlertDialog(
            onDismissRequest = { onStopPickingFolder() },
            title = { Text("Select Destination", fontWeight = FontWeight.Bold) },
            text = { Text("Extract '${pickingFolderForArchive.name}' to current folder?") },
            confirmButton = {
                Button(onClick = { onExtractToCurrentFolder() }) {
                    Text("Extract Here")
                }
            },
            dismissButton = {
                TextButton(onClick = { onStopPickingFolder() }) {
                    Text("Cancel")
                }
            }
        )
    }

    BackHandler(enabled = isSelectionActive || isSearchActive) {
        if (isSelectionActive) {
            onClearSelection()
        } else if (isSearchActive) {
            isSearchActive = false
            onSetSearchQuery("")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isMoving.isNotEmpty() || isCopying.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMoving.isNotEmpty()) "Moving ${isMoving.size} items" else "Copying ${isCopying.size} items",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onCancelOperation() }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { onPaste() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Paste Here")
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isSelectionActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 12.dp,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SelectionActionButton(
                            drawableRes = R.drawable.copy_outline_icon,
                            label = "Copy",
                            onClick = { onStartCopy(selectedFiles.toList()) }
                        )
                        SelectionActionButton(
                            drawableRes = R.drawable.open_folder_outline_icon,
                            label = "Move",
                            onClick = { onStartMove(selectedFiles.toList()) }
                        )
                        SelectionActionButton(
                            drawableRes = R.drawable.share_icon,
                            label = "Share",
                            onClick = {
                                if (selectedFiles.size == 1) {
                                    FileUtils.shareFile(localContext, selectedFiles.first())
                                }
                            }
                        )
                        SelectionActionButton(
                            drawableRes = R.drawable.delete_icon,
                            label = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { onDeleteSelectedFiles() }
                        )
                    }
                }
            }
        },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                if (isSearchActive) {
                    SearchTopBar(
                        query = searchQuery,
                        onQueryChange = { onSetSearchQuery(it) },
                        onCloseClick = {
                            isSearchActive = false
                            onSetSearchQuery("")
                        }
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isSelectionActive) "${selectedFiles.size} items" else title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isSelectionActive) {
                                    onClearSelection()
                                } else {
                                    onBackClick()
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSelectionActive) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            if (isSelectionActive) {
                                if (uiState is ExplorerUiState.Success) {
                                    val filesList = uiState.files
                                    val isAllSelected = selectedFiles.size == filesList.size && filesList.isNotEmpty()
                                    IconButton(onClick = {
                                        if (isAllSelected) onClearSelection() else onSelectAllPaths(filesList.map { it.path })
                                    }) {
                                        Icon(
                                            Icons.Rounded.SelectAll,
                                            contentDescription = "Select All",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.magnifying_glass_icon),
                                        contentDescription = "Search",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Box {
                                    IconButton(onClick = { showMenu = !showMenu }) {
                                        Icon(
                                            Icons.Rounded.MoreVert,
                                            contentDescription = "More",
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Refresh") },
                                            onClick = {
                                                showMenu = false
                                                onRefresh()
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.Refresh,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Sort Options") },
                                            onClick = {
                                                showMenu = false
                                                showSortSheet = true
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.SortByAlpha,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("View Layout") },
                                            onClick = {
                                                showMenu = false
                                                showViewOptionsSheet = true
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.GridView,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
            when (uiState) {
                is ExplorerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is ExplorerUiState.Success -> {
                    val files = uiState.files
                    FileList(
                        files = files,
                        selectedFiles = selectedFiles,
                        isSelectionActive = isSelectionActive,
                        viewMode = viewMode,
                        breadcrumbsList = breadcrumbsList,
                        sortType = sortType,
                        sortOrder = sortOrder,
                        onBreadcrumbClick = onBreadcrumbClick,
                        onSortClick = { showSortSheet = true },
                        onViewModeClick = { showViewOptionsSheet = true },
                        onFileClick = { file ->
                            if (isSelectionActive) {
                                onToggleFileSelection(file.path)
                            } else {
                                onFileClick(file)
                            }
                        },
                        onFileLongClick = { onToggleFileSelection(it.path) },
                        onFileActionsClick = { fileForActions = it },
                        onSelectionToggle = { onToggleFileSelection(it.path) },
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }
                is ExplorerUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(uiState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionActionButton(
    imageVector: ImageVector? = null,
    drawableRes: Int? = null,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        if (drawableRes != null) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        } else if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
fun FileList(
    files: List<FileModel>,
    selectedFiles: Set<String>,
    isSelectionActive: Boolean,
    viewMode: ViewMode,
    breadcrumbsList: List<BreadcrumbItem>,
    sortType: SortType,
    sortOrder: SortOrder,
    onBreadcrumbClick: (BreadcrumbItem) -> Unit,
    onSortClick: () -> Unit,
    onViewModeClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    onFileLongClick: (FileModel) -> Unit,
    onFileActionsClick: (FileModel) -> Unit,
    onSelectionToggle: (FileModel) -> Unit,
    bottomPadding: Dp
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Horizontal padding ko zero rakha hai taake headers full width occupy karein
    val finalContentPadding = remember(bottomPadding, navBarBottom) {
        PaddingValues(
            start = 0.dp,
            end = 0.dp,
            top = 4.dp,
            bottom = bottomPadding + navBarBottom + 24.dp
        )
    }

    val currentOnFileClick by rememberUpdatedState(onFileClick)
    val currentOnFileLongClick by rememberUpdatedState(onFileLongClick)
    val currentOnFileActionsClick by rememberUpdatedState(onFileActionsClick)
    val currentOnSelectionToggle by rememberUpdatedState(onSelectionToggle)

    Box(modifier = Modifier.fillMaxSize()) {
        if (files.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Breadcrumbs(items = breadcrumbsList, onItemClick = onBreadcrumbClick)
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    SortBar(
                        currentType = sortType,
                        currentOrder = sortOrder,
                        viewMode = viewMode,
                        onSortClick = onSortClick,
                        onViewModeClick = onViewModeClick
                    )
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No files found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (viewMode == ViewMode.SMALL || viewMode == ViewMode.LIST) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = finalContentPadding,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header 1: Breadcrumbs
                item(key = "breadcrumbs_header") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Breadcrumbs(items = breadcrumbsList, onItemClick = onBreadcrumbClick)
                    }
                }

                // Header 2: Sort Bar & Divider
                item(key = "sortbar_header") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SortBar(
                            currentType = sortType,
                            currentOrder = sortOrder,
                            viewMode = viewMode,
                            onSortClick = onSortClick,
                            onViewModeClick = onViewModeClick
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }

                // File Items (Horizontal Padding is for individual rows)
                items(
                    items = files,
                    key = { file -> file.path },
                    contentType = { file -> if (file.isDirectory) "folder" else "file" }
                ) { file ->
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        FileRowItem(
                            file = file,
                            isSelected = selectedFiles.contains(file.path),
                            isSelectionActive = isSelectionActive,
                            onClick = { currentOnFileClick(file) },
                            onLongClick = { currentOnFileLongClick(file) },
                            onActionsClick = { currentOnFileActionsClick(file) },
                            onSelectionToggle = { currentOnSelectionToggle(file) }
                        )
                    }
                }
            }
        } else {
            val columnsCount = if (viewMode == ViewMode.MEDIUM) 3 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = finalContentPadding.calculateBottomPadding()
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Span 1: Breadcrumbs
                item(span = { GridItemSpan(columnsCount) }, key = "breadcrumbs_header") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Breadcrumbs(items = breadcrumbsList, onItemClick = onBreadcrumbClick)
                    }
                }

                // Header Span 2: Sort Bar & Divider
                item(span = { GridItemSpan(columnsCount) }, key = "sortbar_header") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SortBar(
                            currentType = sortType,
                            currentOrder = sortOrder,
                            viewMode = viewMode,
                            onSortClick = onSortClick,
                            onViewModeClick = onViewModeClick
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }

                // File Grid Items
                items(
                    items = files,
                    key = { file -> file.path },
                    contentType = { file -> if (file.isDirectory) "folder" else "file" }
                ) { file ->
                    FileGridItem(
                        file = file,
                        viewMode = viewMode,
                        isSelected = selectedFiles.contains(file.path),
                        onClick = { currentOnFileClick(file) },
                        onLongClick = { currentOnFileLongClick(file) },
                        onActionsClick = { currentOnFileActionsClick(file) },
                        onSelectionToggle = { currentOnSelectionToggle(file) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRowItem(
    file: FileModel,
    isSelected: Boolean,
    isSelectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onActionsClick: () -> Unit,
    onSelectionToggle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        else
            Color.Transparent,
        label = "containerColor"
    )

    val subtitleText = remember(file.formattedSize, file.formattedDate, file.itemCount, file.isDirectory, file.size, file.lastModified) {
        if (file.formattedSize.isNotEmpty() && file.formattedDate.isNotEmpty()) {
            if (file.isDirectory) "${file.itemCount} Items" else "${file.formattedSize} • ${file.formattedDate}"
        } else {
            if (file.isDirectory) "${file.itemCount} Items" else "${FileUtils.formatFileSize(file.size)} • ${FileUtils.formatDate(file.lastModified)}"
        }
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileThumbnail(file, isGrid = false)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelectionActive || isSelected) {
                IconButton(onClick = onSelectionToggle) {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(id = R.drawable.approve_accept_icon),
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = "Unselected",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                IconButton(onClick = onActionsClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FileModel,
    viewMode: ViewMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onActionsClick: () -> Unit,
    onSelectionToggle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val itemHeight = if (viewMode == ViewMode.LARGE) 130.dp else 105.dp

    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        else
            MaterialTheme.colorScheme.surface,
        label = "gridContainerColor"
    )

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FileThumbnail(file, isGrid = true)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onSelectionToggle() }
                    )
                } else {
                    IconButton(
                        onClick = onActionsClick,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionBottomSheet(
    file: FileModel,
    onDismiss: () -> Unit,
    onRenameClick: (FileModel) -> Unit,
    onMoveClick: (FileModel) -> Unit,
    onCopyClick: (FileModel) -> Unit,
    onDeleteClick: (FileModel) -> Unit,
    onExtractClick: (FileModel) -> Unit,
    onFavoriteClick: (FileModel) -> Unit,
    onLockClick: (FileModel) -> Unit,
    onShareClick: (FileModel) -> Unit,
    onOpenWithClick: (FileModel) -> Unit,
    onSelectClick: (FileModel) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileThumbnail(file = file, isGrid = false)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (file.isDirectory) "${file.itemCount} Items" else FileUtils.formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val ext = remember(file.extension) { file.extension.lowercase() }

            FileActionItem(imageVector = Icons.AutoMirrored.Rounded.OpenInNew, label = "Open With") { onOpenWithClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.share_icon, label = "Share") { onShareClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.rename_icon, label = "Rename") { onRenameClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.open_folder_outline_icon, label = "Move") { onMoveClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.copy_outline_icon, label = "Copy") { onCopyClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.approve_accept_icon, label = "Select") { onSelectClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.heart_thin_icon, label = "Favorite", tint = Color(0xFFFFB300)) { onFavoriteClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.shield_lock_line_icon, label = "Move to Vault") { onLockClick(file); onDismiss() }

            if (ext in listOf("zip", "rar", "7z")) {
                FileActionItem(drawableRes = R.drawable.archive_line_icon, label = "Extract Here") { onExtractClick(file); onDismiss() }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            FileActionItem(drawableRes = R.drawable.delete_icon, label = "Delete", tint = MaterialTheme.colorScheme.error) { onDeleteClick(file); onDismiss() }
        }
    }
}

@Composable
private fun FileActionItem(
    imageVector: ImageVector? = null,
    drawableRes: Int? = null,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label, color = tint, fontWeight = FontWeight.Medium) },
        leadingContent = {
            if (drawableRes != null) {
                Icon(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            } else if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier.combinedClickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
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
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Close Search"
                )
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Rounded.Clear,
                        contentDescription = "Clear"
                    )
                }
            }
        }
    )
}

@Composable
fun Breadcrumbs(
    items: List<BreadcrumbItem>,
    onItemClick: (BreadcrumbItem) -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isFirst = index == 0
            val isLast = index == items.lastIndex

            if (!isFirst) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isLast) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                onClick = { onItemClick(item) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFirst && item.category == null) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
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
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                if (currentOrder == SortOrder.ASCENDING) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = null,
                modifier = Modifier
                    .size(14.dp)
                    .padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onViewModeClick, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = when(viewMode) {
                    ViewMode.SMALL -> Icons.AutoMirrored.Rounded.ViewList
                    ViewMode.MEDIUM -> Icons.Rounded.GridView
                    ViewMode.LARGE -> Icons.Rounded.ViewModule
                    else -> Icons.AutoMirrored.Rounded.ViewList
                },
                contentDescription = "Switch View",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Rename", color = MaterialTheme.colorScheme.onPrimary)
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
fun FileThumbnail(file: FileModel, isGrid: Boolean) {
    val localContext = LocalContext.current
    val ext = remember(file.extension) { file.extension.lowercase() }

    val isVideo = remember(ext) { ext in listOf("mp4", "mkv", "avi", "mov", "3gp", "webm") }
    val isImage = remember(ext) { ext in listOf("jpg", "jpeg", "png", "webp", "gif", "heic") }
    val isApk = ext == "apk"
    val thumbSize = if (isGrid) 44.dp else 40.dp

    if (isVideo || isImage || isApk) {
        val imageRequest = remember(file.path, isVideo) {
            ImageRequest.Builder(localContext)
                .data(file.path)
                .apply {
                    if (isVideo) {
                        videoFrameMillis(1000)
                    }
                }
                .crossfade(false)
                .size(96, 96)
                .precision(Precision.EXACT)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val painter = rememberAsyncImagePainter(model = imageRequest)

            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.video_playlist_icon),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        val (icon, color) = rememberFileTypeIconAndColor(file.isDirectory, ext)
        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun rememberFileTypeIconAndColor(isDirectory: Boolean, ext: String): Pair<Painter, Color> {
    val folderIcon = painterResource(R.drawable.folder_icon)
    val docIcon = painterResource(R.drawable.text_document_line_icon)
    val audioIcon = painterResource(R.drawable.audio_tune_icon)
    val archiveIcon = painterResource(R.drawable.archive_line_icon)

    return remember(isDirectory, ext) {
        when {
            isDirectory -> folderIcon to Color(0xFF64B5F6)
            ext == "pdf" -> docIcon to Color(0xFFEF5350)
            ext in listOf("doc", "docx") -> docIcon to Color(0xFF1E88E5)
            ext in listOf("xls", "xlsx") -> docIcon to Color(0xFF43A047)
            ext in listOf("ppt", "pptx") -> docIcon to Color(0xFFF4511E)
            ext in listOf("mp3", "wav", "flac", "opus", "ogg") -> audioIcon to Color(0xFF66BB6A)
            ext in listOf("zip", "rar", "7z") -> archiveIcon to Color(0xFF78909C)
            else -> docIcon to Color(0xFFBDBDBD)
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
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Icon(
                imageVector = if (order == SortOrder.ASCENDING) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
        ViewMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =========================================================================
// PREVIEW COMPOSABLES
// =========================================================================

private val previewMockFiles = listOf(
    FileModel(
        path = "/storage/emulated/0/Documents",
        name = "Documents Folder",
        isDirectory = true,
        size = 0L,
        lastModified = System.currentTimeMillis(),
        itemCount = 12
    ),
    FileModel(
        path = "/storage/emulated/0/Pictures/photo.jpg",
        name = "Sample_Image.jpg",
        isDirectory = false,
        size = 2450000L,
        lastModified = System.currentTimeMillis() - 86400000,
        extension = "jpg"
    ),
    FileModel(
        path = "/storage/emulated/0/Download/document.pdf",
        name = "Project_Report.pdf",
        isDirectory = false,
        size = 10485760L,
        lastModified = System.currentTimeMillis() - 172800000,
        extension = "pdf"
    )
)

private val previewBreadcrumbs = listOf(
    BreadcrumbItem(name = "Home", path = ""),
    BreadcrumbItem(name = "Internal Storage", path = "/storage/emulated/0"),
    BreadcrumbItem(name = "Documents", path = "/storage/emulated/0/Documents")
)

@Preview(name = "Normal Screen - Light Mode", showBackground = true)
@Preview(name = "Normal Screen - Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun ExplorerScreenNormalPreview() {
    MaterialTheme {
        ExplorerScreenContent(
            title = "Internal Storage",
            uiState = ExplorerUiState.Success(previewMockFiles),
            selectedFiles = emptySet(),
            searchQuery = "",
            sortType = SortType.NAME,
            sortOrder = SortOrder.ASCENDING,
            viewMode = ViewMode.SMALL,
            breadcrumbsList = previewBreadcrumbs,
            isMoving = emptyList(),
            isCopying = emptyList(),
            pickingFolderForArchive = null,
            onBackClick = {},
            onFileClick = {},
            onRenameFile = { _, _ -> },
            onSetSort = { _, _ -> },
            onSetViewMode = {},
            onStopPickingFolder = {},
            onExtractToCurrentFolder = {},
            onClearSelection = {},
            onSetSearchQuery = {},
            onDeleteSelectedFiles = {},
            onStartMove = {},
            onStartCopy = {},
            onPaste = {},
            onCancelOperation = {},
            onRefresh = {},
            onSelectAllPaths = {},
            onToggleFileSelection = {},
            onToggleFavorite = {},
            onExtractArchive = {},
            onMoveToVault = {},
            onBreadcrumbClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Selection Mode Active - Bottom Bar Lifted", showBackground = true)
@Composable
fun ExplorerScreenSelectionActivePreview() {
    MaterialTheme {
        ExplorerScreenContent(
            title = "Internal Storage",
            uiState = ExplorerUiState.Success(previewMockFiles),
            selectedFiles = setOf(previewMockFiles[1].path),
            searchQuery = "",
            sortType = SortType.NAME,
            sortOrder = SortOrder.ASCENDING,
            viewMode = ViewMode.SMALL,
            breadcrumbsList = previewBreadcrumbs,
            isMoving = emptyList(),
            isCopying = emptyList(),
            pickingFolderForArchive = null,
            onBackClick = {},
            onFileClick = {},
            onRenameFile = { _, _ -> },
            onSetSort = { _, _ -> },
            onSetViewMode = {},
            onStopPickingFolder = {},
            onExtractToCurrentFolder = {},
            onClearSelection = {},
            onSetSearchQuery = {},
            onDeleteSelectedFiles = {},
            onStartMove = {},
            onStartCopy = {},
            onPaste = {},
            onCancelOperation = {},
            onRefresh = {},
            onSelectAllPaths = {},
            onToggleFileSelection = {},
            onToggleFavorite = {},
            onExtractArchive = {},
            onMoveToVault = {},
            onBreadcrumbClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "File Action Sheet Preview", showBackground = true)
@Composable
fun FileActionBottomSheetPreview() {
    MaterialTheme {
        Surface {
            FileActionBottomSheet(
                file = previewMockFiles[1],
                onDismiss = {},
                onRenameClick = {},
                onMoveClick = {},
                onCopyClick = {},
                onDeleteClick = {},
                onExtractClick = {},
                onFavoriteClick = {},
                onLockClick = {},
                onShareClick = {},
                onOpenWithClick = {},
                onSelectClick = {}
            )
        }
    }
}