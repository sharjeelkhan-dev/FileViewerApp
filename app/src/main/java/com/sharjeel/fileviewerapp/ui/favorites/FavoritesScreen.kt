package com.sharjeel.fileviewerapp.ui.favorites

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.components.AppScaffold
import com.sharjeel.fileviewerapp.ui.explorer.*
import com.sharjeel.fileviewerapp.R

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
        searchQuery = searchQuery,
        breadcrumbsList = breadcrumbsList,
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
    searchQuery: String,
    breadcrumbsList: List<BreadcrumbItem>,
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    onFileLongClick: (FileModel) -> Unit,
    onToggleFavorite: (FileModel) -> Unit,
    onDeleteSelected: () -> Unit,
    onSortSelected: (SortType, SortOrder) -> Unit,
    onViewModeSelected: (ViewMode) -> Unit
) {
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }

    val isPreview = LocalInspectionMode.current
    val hasContent = (uiState as? ExplorerUiState.Success)?.files?.isNotEmpty() == true

    val shouldShowHeaders = hasContent || selectedFiles.isNotEmpty()

    if (!isPreview) {
        if (showSortSheet && hasContent) {
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

        if (showViewOptionsSheet && hasContent) {
            ViewOptionsBottomSheet(
                currentMode = viewMode,
                onDismiss = { showViewOptionsSheet = false },
                onModeSelected = { mode: ViewMode ->
                    onViewModeSelected(mode)
                    showViewOptionsSheet = false
                }
            )
        }
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (shouldShowHeaders) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedFiles.isNotEmpty()) "${selectedFiles.size} SELECTED" else "FAVORITES",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        if (selectedFiles.isNotEmpty()) {
                            IconButton(onClick = onDeleteSelected) {
                                Icon(
                                    painter = painterResource(id = R.drawable.recycle_bin_line_icon),
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (hasContent) {
                            IconButton(onClick = { showSortSheet = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                                    contentDescription = "Sort"
                                )
                            }
                            IconButton(onClick = { showViewOptionsSheet = true }) {
                                Icon(Icons.Rounded.GridView, contentDescription = "View Options")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is ExplorerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is ExplorerUiState.Success -> {
                    val filteredFiles = if (searchQuery.isBlank()) uiState.files
                    else uiState.files.filter { it.name.contains(searchQuery, ignoreCase = true) }

                    if (filteredFiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No favorites yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Mark files as favorite to see them here",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(28.dp))
                                Button(
                                    onClick = onBackClick,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Go Back")
                                }
                            }
                        }
                    } else {
                        Column {
                            // 🎯 FIXED: Breadcrumbs added
                            Breadcrumbs(
                                items = breadcrumbsList,
                                onItemClick = { }
                            )

                            SortBar(
                                currentType = sortType,
                                currentOrder = sortOrder,
                                viewMode = viewMode,
                                onSortClick = { showSortSheet = true },
                                onViewModeClick = { showViewOptionsSheet = true }
                            )

                            FileList(
                                files = filteredFiles,
                                selectedFiles = selectedFiles,
                                viewMode = viewMode,
                                onFileClick = onFileClick,
                                onFileLongClick = onFileLongClick,
                                onFavoriteClick = { file: FileModel -> onToggleFavorite(file) },
                                onDeleteClick = { onFileLongClick(it) },
                                onRenameClick = { },
                                onShareClick = { },
                                onOpenWithClick = { },
                                onExtractClick = { },
                                onLockClick = { },
                                onMoveClick = { },
                                onCopyClick = { },
                                bottomPadding = 0.dp
                            )
                        }
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

@Preview(showBackground = true)
@Composable
fun FavoritesScreenPreview() {
    val mockFiles = listOf(
        FileModel("Fav_Photo.jpg", "/path/fav.jpg", 1024L, System.currentTimeMillis(), false, "jpg"),
        FileModel("Fav_Doc.pdf", "/path/fav.pdf", 2048L, System.currentTimeMillis(), false, "pdf")
    )
    val mockBreadcrumbs = listOf(
        BreadcrumbItem("Internal Storage", "/root", null),
        BreadcrumbItem("Favorites", "", null)
    )
    MaterialTheme {
        FavoritesContent(
            uiState = ExplorerUiState.Success(files = mockFiles),
            selectedFiles = emptySet(),
            viewMode = ViewMode.LIST,
            sortType = SortType.NAME,
            sortOrder = SortOrder.ASCENDING,
            searchQuery = "",
            breadcrumbsList = mockBreadcrumbs,
            onBackClick = {},
            onFileClick = {},
            onFileLongClick = {},
            onToggleFavorite = {},
            onDeleteSelected = {},
            onSortSelected = { _, _ -> },
            onViewModeSelected = {}
        )
    }
}
