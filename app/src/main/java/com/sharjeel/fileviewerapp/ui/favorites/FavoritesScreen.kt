package com.sharjeel.fileviewerapp.ui.favorites

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
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
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
fun FavoritesScreen(
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
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

    FavoritesContent(
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
        onToggleFavorite = { file: FileModel -> viewModel.toggleFavorite(file) },
        onRemoveSelectedFavorites = { viewModel.removeSelectedFromFavorites() },
        onDeleteSelected = { viewModel.deleteSelectedFiles() },
        onSortSelected = { type: SortType, order: SortOrder -> viewModel.updateSort(type, order) },
        onViewModeSelected = { mode: ViewMode -> viewModel.updateViewMode(mode) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesContent(
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
    onToggleFavorite: (FileModel) -> Unit,
    onRemoveSelectedFavorites: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSortSelected: (SortType, SortOrder) -> Unit,
    onViewModeSelected: (ViewMode) -> Unit
) {
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }
    var fileForActions by remember { mutableStateOf<FileModel?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val localContext = LocalContext.current

    val visibleFiles = (uiState as? ExplorerUiState.Success)?.files?.filter { !it.name.startsWith(".") } ?: emptyList()
    val filteredFiles = if (searchQuery.isBlank()) visibleFiles else visibleFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val isFavoritesEmpty = filteredFiles.isEmpty() && uiState is ExplorerUiState.Success
    val isSelectionActive = selectedFiles.isNotEmpty()

    if (showSortSheet && !isFavoritesEmpty) {
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

    if (showViewOptionsSheet && !isFavoritesEmpty) {
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
        FileActionBottomSheet(
            file = fileForActions!!,
            onDismiss = { fileForActions = null },
            onRenameClick = { },
            onMoveClick = { },
            onCopyClick = { },
            onDeleteClick = { onDeleteSelected() },
            onExtractClick = { },
            onFavoriteClick = { onToggleFavorite(it) },
            onLockClick = { },
            onShareClick = { FileUtils.shareFile(localContext, it.path) },
            onOpenWithClick = { FileUtils.openWithExternalApp(localContext, it.path) },
            onSelectClick = { onFileLongClick(it) }
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
                                text = if (isSelectionActive) "${selectedFiles.size} items" else "Favorites",
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
                            drawableRes = R.drawable.copy_outline_icon,
                            label = "Copy",
                            onClick = { /* Copy Action */ }
                        )
                        SelectionActionButton(
                            drawableRes = R.drawable.open_folder_outline_icon,
                            label = "Move",
                            onClick = { /* Move Action */ }
                        )
                        SelectionActionButton(
                            drawableRes = R.drawable.heart_black_icon,
                            label = "Unfavorite",
                            onClick = { onRemoveSelectedFavorites() }
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
                            onClick = { onDeleteSelected() }
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
                    if (isFavoritesEmpty) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.heart_thin_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching favorites" else "No favorites yet",
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