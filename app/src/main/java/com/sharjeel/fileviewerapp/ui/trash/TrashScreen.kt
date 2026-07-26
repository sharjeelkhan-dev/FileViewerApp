package com.sharjeel.fileviewerapp.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.components.AppScaffold
import com.sharjeel.fileviewerapp.ui.explorer.*
import com.sharjeel.fileviewerapp.util.FileUtils

@Composable
fun TrashScreen(
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val breadcrumbsList by viewModel.breadcrumbs.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = selectedFiles.isNotEmpty() || isSearchActive) {
        if (selectedFiles.isNotEmpty()) {
            viewModel.clearSelection()
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

    TrashContent(
        uiState = uiState,
        selectedFiles = selectedFiles,
        viewMode = viewMode,
        sortType = sortType,
        sortOrder = sortOrder,
        breadcrumbsList = breadcrumbsList,
        isSearchActive = isSearchActive,
        searchQuery = searchQuery,
        onSearchToggle = { active: Boolean ->
            isSearchActive = active
            if (!active) searchQuery = ""
        },
        onSearchQueryChange = { query: String -> searchQuery = query },
        onClearSelection = { viewModel.clearSelection() },
        onSelectAll = { viewModel.selectAll() },
        onBackClick = onBackClick,
        onFileClick = { file: FileModel ->
            if (selectedFiles.isNotEmpty()) {
                viewModel.toggleFileSelection(file.path)
            } else {
                onFileClick(file)
            }
        },
        onFileLongClick = { file: FileModel -> viewModel.toggleFileSelection(file.path) },
        onRestoreFile = { file: FileModel -> viewModel.restoreFile(file) },
        onRestoreSelected = { viewModel.restoreSelected() },
        onDeleteFilePermanently = { file: FileModel -> viewModel.deletePermanently(file) },
        onDeleteSelectedPermanently = { viewModel.deleteSelectedPermanently() },
        onEmptyTrash = { viewModel.emptyTrash() },
        onSortSelected = { type: SortType, order: SortOrder -> viewModel.updateSort(type, order) },
        onViewModeSelected = { mode: ViewMode -> viewModel.updateViewMode(mode) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashContent(
    uiState: ExplorerUiState,
    selectedFiles: Set<String>,
    viewMode: ViewMode,
    sortType: SortType,
    sortOrder: SortOrder,
    breadcrumbsList: List<BreadcrumbItem>,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchToggle: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    onFileLongClick: (FileModel) -> Unit,
    onRestoreFile: (FileModel) -> Unit,
    onRestoreSelected: () -> Unit,
    onDeleteFilePermanently: (FileModel) -> Unit,
    onDeleteSelectedPermanently: () -> Unit,
    onEmptyTrash: () -> Unit,
    onSortSelected: (SortType, SortOrder) -> Unit,
    onViewModeSelected: (ViewMode) -> Unit
) {
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }
    var fileForActions by remember { mutableStateOf<FileModel?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val visibleFiles = (uiState as? ExplorerUiState.Success)?.files?.filter { !it.name.startsWith(".") } ?: emptyList()
    val filteredFiles = if (searchQuery.isBlank()) visibleFiles else visibleFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val isTrashEmpty = filteredFiles.isEmpty() && uiState is ExplorerUiState.Success
    val isSelectionActive = selectedFiles.isNotEmpty()

    // --- Dialogs ---
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete all items in the trash?") },
            confirmButton = {
                TextButton(onClick = {
                    onEmptyTrash()
                    showEmptyTrashDialog = false
                }) {
                    Text("Empty", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Permanently", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete ${selectedFiles.size} item(s)?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSelectedPermanently()
                    showDeleteConfirmDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- BottomSheets ---
    if (showSortSheet && !isTrashEmpty) {
        SortBottomSheet(
            currentType = sortType,
            currentOrder = sortOrder,
            onDismiss = { showSortSheet = false },
            onSortSelected = { type: SortType, order: SortOrder ->
                onSortSelected(type, order)
                showSortSheet = false
            }
        )
    }

    if (showViewOptionsSheet && !isTrashEmpty) {
        ViewOptionsBottomSheet(
            currentMode = viewMode,
            onDismiss = { showViewOptionsSheet = false },
            onModeSelected = { mode: ViewMode ->
                onViewModeSelected(mode)
                showViewOptionsSheet = false
            }
        )
    }

    if (fileForActions != null) {
        TrashFileActionBottomSheet(
            file = fileForActions!!,
            onDismiss = { fileForActions = null },
            onRestoreClick = {
                onRestoreFile(it)
                fileForActions = null
            },
            onDeletePermanentlyClick = {
                onDeleteFilePermanently(it)
                fileForActions = null
            },
            onSelectClick = {
                onFileLongClick(it)
                fileForActions = null
            }
        )
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                if (isSearchActive) {
                    SearchTopBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onCloseClick = { onSearchToggle(false) }
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isSelectionActive) "${selectedFiles.size} items" else "Trash",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (isSelectionActive) onClearSelection() else onBackClick()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelectionActive) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            if (isSelectionActive) {
                                val isAllSelected = selectedFiles.size == filteredFiles.size && filteredFiles.isNotEmpty()
                                IconButton(onClick = { if (isAllSelected) onClearSelection() else onSelectAll() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.SelectAll,
                                        contentDescription = "Select All",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                IconButton(onClick = { onSearchToggle(true) }) {
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
                                            text = { Text("Empty Trash") },
                                            onClick = {
                                                showMenu = false
                                                showEmptyTrashDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.recycle_bin_line_icon),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
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
        bottomBar = {
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
                            drawableRes = R.drawable.reload_sync_icon,
                            label = "Restore",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { onRestoreSelected() }
                        )
                        SelectionActionButton(
                            drawableRes = R.drawable.delete_icon,
                            label = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteConfirmDialog = true }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            when (uiState) {
                is ExplorerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is ExplorerUiState.Success -> {
                    if (isTrashEmpty) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.delete_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching items in trash" else "Trash is empty",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else {
                        FileList(
                            files = filteredFiles,
                            selectedFiles = selectedFiles,
                            isSelectionActive = isSelectionActive,
                            viewMode = viewMode,
                            sortType = sortType,
                            sortOrder = sortOrder,
                            breadcrumbsList = breadcrumbsList,
                            onFileClick = onFileClick,
                            onFileLongClick = onFileLongClick,
                            onFileActionsClick = { fileForActions = it },
                            onBreadcrumbClick = { item -> if (item.path.isEmpty()) onBackClick() },
                            onSortClick = { showSortSheet = true },
                            onViewModeClick = { showViewOptionsSheet = true },
                            onSelectionToggle = { file -> onFileLongClick(file) },
                            bottomPadding = paddingValues.calculateBottomPadding()
                        )
                    }
                }
                is ExplorerUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashFileActionBottomSheet(
    file: FileModel,
    onDismiss: () -> Unit,
    onRestoreClick: (FileModel) -> Unit,
    onDeletePermanentlyClick: (FileModel) -> Unit,
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

            FileActionItem(drawableRes = R.drawable.reload_sync_icon, label = "Restore", tint = MaterialTheme.colorScheme.primary) { onRestoreClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.approve_accept_icon, label = "Select") { onSelectClick(file); onDismiss() }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            FileActionItem(drawableRes = R.drawable.delete_icon, label = "Delete Permanently", tint = MaterialTheme.colorScheme.error) { onDeletePermanentlyClick(file); onDismiss() }
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
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
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
            .padding(horizontal = 10.dp, vertical = 6.dp)
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