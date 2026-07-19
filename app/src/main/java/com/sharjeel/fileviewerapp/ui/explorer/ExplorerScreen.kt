package com.sharjeel.fileviewerapp.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MoveUp
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import androidx.compose.ui.tooling.preview.Preview
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
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
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExplorerEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ExplorerEvent.NavigateToFolder -> {
                    onPathClick(event.path)
                }
                ExplorerEvent.NavigateToHome -> {
                    onHomeClick()
                }
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
        onExtractArchive = { viewModel.extractArchive(context, it) },
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
    val context = LocalContext.current

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
                onRenameFile(fileToRename!!.path, newName)
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
            title = { Text("Select Destination") },
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

    BackHandler(enabled = selectedFiles.isNotEmpty() || isSearchActive) {
        if (selectedFiles.isNotEmpty()) {
            onClearSelection()
        } else if (isSearchActive) {
            isSearchActive = false
            onSetSearchQuery("")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                if (selectedFiles.isNotEmpty()) "${selectedFiles.size} Selected" else title.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (selectedFiles.isNotEmpty()) {
                                    onClearSelection()
                                } else {
                                    onBackClick()
                                }
                            }) {
                                Icon(
                                    if (selectedFiles.isNotEmpty()) Icons.Rounded.Close
                                    else Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back/Close",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            if (selectedFiles.isNotEmpty()) {
                                IconButton(onClick = { onDeleteSelectedFiles() }) {
                                    Icon(
                                        painterResource(id = R.drawable.recycle_bin_line_icon),
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(onClick = { onStartMove(selectedFiles.toList()) }) {
                                    Icon(Icons.Rounded.MoveUp, contentDescription = "Move")
                                }
                                IconButton(onClick = { onStartCopy(selectedFiles.toList()) }) {
                                    Icon(painterResource(R.drawable.copy_outline_icon), contentDescription = "Copy", modifier = Modifier.size(20.dp))
                                }
                            }
                            if (isMoving.isNotEmpty() || isCopying.isNotEmpty()) {
                                IconButton(onClick = { onPaste() }) {
                                    Icon(painterResource(R.drawable.shortcut_icon), contentDescription = "Paste", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onCancelOperation() }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                                }
                            }
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
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
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
                                    if (uiState is ExplorerUiState.Success) {
                                        val filesList = uiState.files
                                        val isAllSelected = selectedFiles.size == filesList.size && filesList.isNotEmpty()

                                        DropdownMenuItem(
                                            text = { Text(if (isAllSelected) "Clear Selection" else "Select All") },
                                            onClick = {
                                                showMenu = false
                                                if (isAllSelected) {
                                                    onClearSelection()
                                                } else {
                                                    val allPaths = filesList.map { it.path }
                                                    onSelectAllPaths(allPaths)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    if (isAllSelected) Icons.Rounded.Close
                                                    else Icons.Rounded.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Breadcrumbs(
                items = breadcrumbsList,
                onItemClick = onBreadcrumbClick
            )

            SortBar(
                currentType = sortType,
                currentOrder = sortOrder,
                viewMode = viewMode,
                onSortClick = { showSortSheet = true },
                onViewModeClick = { showViewOptionsSheet = true }
            )
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-10).dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState) {
                    is ExplorerUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is ExplorerUiState.Success -> {
                        if (uiState.files.isEmpty()) {
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
                        } else {
                            FileList(
                                files = uiState.files,
                                selectedFiles = selectedFiles,
                                viewMode = viewMode,
                                onFileClick = { file ->
                                    if (selectedFiles.isNotEmpty()) {
                                        onToggleFileSelection(file.path)
                                    } else {
                                        onFileClick(file)
                                    }
                                },
                                onFileLongClick = { onToggleFileSelection(it.path) },
                                onDeleteClick = { onToggleFileSelection(it.path); onDeleteSelectedFiles() },
                                onRenameClick = { fileToRename = it },
                                onShareClick = { FileUtils.shareFile(context, it.path) },
                                onOpenWithClick = { FileUtils.openWithExternalApp(context, it.path) },
                                onFavoriteClick = { onToggleFavorite(it) },
                                onExtractClick = { onExtractArchive(it.path) },
                                onLockClick = { onMoveToVault(it) },
                                onMoveClick = { onStartMove(listOf(it.path)) },
                                onCopyClick = { onStartCopy(listOf(it.path)) },
                                bottomPadding = innerPadding.calculateBottomPadding()
                            )
                        }
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
    val scrollState = androidx.compose.foundation.rememberScrollState()

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isFirst = index == 0
            val isLast = index == items.lastIndex

            if (!isFirst) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_right_direction_icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    isFirst && item.category == null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    isLast -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                onClick = { onItemClick(item) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFirst && item.category == null) {
                        Icon(
                            painter = painterResource(id = R.drawable.house_window_icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Bold,
                        color = when {
                            isFirst && item.category == null -> MaterialTheme.colorScheme.onPrimaryContainer
                            isLast -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
            .offset(y = (-10).dp)
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
fun FileList(
    files: List<FileModel>,
    selectedFiles: Set<String>,
    viewMode: ViewMode,
    onFileClick: (FileModel) -> Unit,
    onFileLongClick: (FileModel) -> Unit,
    onDeleteClick: (FileModel) -> Unit,
    onRenameClick: (FileModel) -> Unit,
    onShareClick: (FileModel) -> Unit,
    onOpenWithClick: (FileModel) -> Unit,
    onFavoriteClick: (FileModel) -> Unit,
    onExtractClick: (FileModel) -> Unit,
    onLockClick: (FileModel) -> Unit,
    onMoveClick: (FileModel) -> Unit,
    onCopyClick: (FileModel) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val finalContentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 4.dp,
        bottom = bottomPadding + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
    )

    if (viewMode == ViewMode.SMALL) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = finalContentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(files) { file ->
                FileRowItem(
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
                    onLock = { onLockClick(file) },
                    onMove = { onMoveClick(file) },
                    onCopy = { onCopyClick(file) }
                )
            }
        }
    } else {
        val columnsCount = if (viewMode == ViewMode.MEDIUM) 3 else 2
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsCount),
            modifier = Modifier.fillMaxSize(),
            contentPadding = finalContentPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(files.size) { index ->
                val file = files[index]
                FileGridItem(
                    file = file,
                    viewMode = viewMode,
                    isSelected = selectedFiles.contains(file.path),
                    onClick = { onFileClick(file) },
                    onLongClick = { onFileLongClick(file) },
                    onDelete = { onDeleteClick(file) },
                    onRename = { onRenameClick(file) },
                    onShare = { onShareClick(file) },
                    onOpenWith = { onOpenWithClick(file) },
                    onFavorite = { onFavoriteClick(file) },
                    onExtract = { onExtractClick(file) },
                    onLock = { onLockClick(file) },
                    onMove = { onMoveClick(file) },
                    onCopy = { onCopyClick(file) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRowItem(
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
    onLock: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp,
            MaterialTheme.colorScheme.primary) else null,
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
            FileThumbnail(file, isGrid = false)
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
            ActionMenuButton(file, isSelected, onLongClick, onDelete, onRename, onMove, onCopy, onExtract, onFavorite, onLock, onShare, onOpenWith)
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
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onFavorite: () -> Unit,
    onExtract: () -> Unit,
    onLock: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit
) {
    val itemHeight = if (viewMode == ViewMode.LARGE) 150.dp else 120.dp

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp,
            MaterialTheme.colorScheme.primary) else null,
        shadowElevation = if (isSelected) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FileThumbnail(file, isGrid = true)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                ActionMenuButton(file, isSelected, onLongClick, onDelete, onRename, onMove, onCopy, onExtract, onFavorite, onLock, onShare, onOpenWith)
            }
        }
    }
}

@Composable
fun ActionMenuButton(
    file: FileModel,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onExtract: () -> Unit,
    onFavorite: () -> Unit,
    onLock: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit
) {
    if (isSelected) {
        Icon(painter = painterResource(id = R.drawable.check_mark_circle_line_icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    } else {
        var showFileMenu by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { showFileMenu = true }, modifier = Modifier.size(24.dp)) {
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
                shape = RoundedCornerShape(24.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Rename", fontWeight = FontWeight.SemiBold) },
                    onClick = { onRename(); showFileMenu = false },
                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.brush_paintbrush_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Move", fontWeight = FontWeight.SemiBold) },
                    onClick = { onMove(); showFileMenu = false },
                    leadingIcon = { Icon(Icons.Rounded.MoveUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Copy", fontWeight = FontWeight.SemiBold) },
                    onClick = { onCopy(); showFileMenu = false },
                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.copy_outline_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Delete", fontWeight = FontWeight.SemiBold) },
                    onClick = { onDelete(); showFileMenu = false },
                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.recycle_bin_line_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)) }
                )
                if (file.extension.lowercase() in listOf("zip", "rar")) {
                    DropdownMenuItem(
                        text = { Text("Extract Here", fontWeight = FontWeight.SemiBold) },
                        onClick = { onExtract(); showFileMenu = false },
                        leadingIcon = { Icon(painter = painterResource(id = R.drawable.check_mark_circle_line_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Select", fontWeight = FontWeight.SemiBold) },
                    onClick = { onLongClick(); showFileMenu = false },
                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.check_mark_circle_line_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Favorite", fontWeight = FontWeight.SemiBold) },
                    onClick = { onFavorite(); showFileMenu = false },
                    leadingIcon = { Icon(Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF4081),
                        modifier = Modifier.size(20.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Lock (Vault)", fontWeight = FontWeight.SemiBold) },
                    onClick = { onLock(); showFileMenu = false },
                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.lock_line_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Share", fontWeight = FontWeight.SemiBold) },
                    onClick = { onShare(); showFileMenu = false },
                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.share_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Open with", fontWeight = FontWeight.SemiBold) },
                    onClick = { onOpenWith(); showFileMenu = false },
                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.shortcut_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)) }
                )
            }
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
    val isVideo = FileUtils.isVideoFile(file.path)
    val isImage = FileUtils.isImageFile(file.path)
    val isApk = file.extension.lowercase() == "apk"
    val thumbSize = if (isGrid) 56.dp else 48.dp

    if (isVideo || isImage || isApk) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(thumbSize),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            AsyncImage(
                model = file.path,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(R.drawable.photo_collage_icon),
                error = painterResource(if (isApk) R.drawable.archive_line_icon else R.drawable.photo_collage_icon)
            )
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
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
            file.isDirectory -> painterResource(R.drawable.folder_icon) to Color(0xFF64B5F6)
            file.extension.lowercase() == "pdf" -> painterResource(R.drawable.text_document_line_icon) to Color(0xFFEF5350)
            file.extension.lowercase() in listOf("doc", "docx") -> painterResource(R.drawable.text_document_line_icon) to Color(0xFF1E88E5)
            file.extension.lowercase() in listOf("xls", "xlsx") -> painterResource(R.drawable.text_document_line_icon) to Color(0xFF43A047)
            file.extension.lowercase() in listOf("ppt", "pptx") -> painterResource(R.drawable.text_document_line_icon) to Color(0xFFF4511E)
            file.extension.lowercase() in listOf("mp3", "wav", "flac", "opus", "ogg") -> painterResource(R.drawable.audio_tune_icon) to Color(0xFF66BB6A)
            file.extension.lowercase() in listOf("zip", "rar", "7z") -> painterResource(R.drawable.archive_line_icon) to Color(0xFF78909C)
            else -> painterResource(R.drawable.text_document_line_icon) to Color(0xFFBDBDBD)
        }
        Box(
            modifier = Modifier
                .size(thumbSize)
                .background(
                    color.copy(alpha = 0.15f),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
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
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
        border = androidx.compose.foundation.BorderStroke(
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

@Preview(showBackground = true)
@Composable
fun ExplorerScreenPreview() {
    val mockFiles = listOf(
        FileModel("Documents", "/Documents", 0, System.currentTimeMillis(), true),
        FileModel("Images", "/Images", 0, System.currentTimeMillis(), true),
        FileModel("report.pdf", "/report.pdf", 1024L * 1024, System.currentTimeMillis(), false, "pdf"),
        FileModel("vacation.jpg", "/vacation.jpg", 2L * 1024 * 1024, System.currentTimeMillis(), false, "jpg")
    )
    val mockBreadcrumbs = listOf(
        BreadcrumbItem("Home", "", null),
        BreadcrumbItem("Internal Storage", "/storage/emulated/0", null)
    )

    FileViewerAppTheme {
        ExplorerScreenContent(
            title = "Explorer",
            uiState = ExplorerUiState.Success(mockFiles),
            selectedFiles = emptySet(),
            searchQuery = "",
            sortType = SortType.NAME,
            sortOrder = SortOrder.ASCENDING,
            viewMode = ViewMode.LIST,
            breadcrumbsList = mockBreadcrumbs,
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
