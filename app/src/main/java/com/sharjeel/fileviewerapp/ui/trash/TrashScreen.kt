package com.sharjeel.fileviewerapp.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.explorer.*
import com.sharjeel.fileviewerapp.ui.components.AppScaffold
import androidx.compose.ui.tooling.preview.Preview
import com.sharjeel.fileviewerapp.R

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
        searchQuery = searchQuery,
        breadcrumbsList = breadcrumbsList,
        onBackClick = onBackClick,
        onFileClick = { file ->
            if (selectedFiles.isNotEmpty()) {
                viewModel.toggleFileSelection(file.path)
            } else {
                onFileClick(file)
            }
        },
        onFileLongClick = { viewModel.toggleFileSelection(it.path) },
        onRestoreSelected = { viewModel.restoreSelected() },
        onDeleteSelectedPermanently = { viewModel.deleteSelectedPermanently() },
        onEmptyTrash = { viewModel.emptyTrash() },
        onSortSelected = { type, order -> viewModel.updateSort(type, order) },
        onViewModeSelected = { mode -> viewModel.updateViewMode(mode) }
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
    searchQuery: String,
    breadcrumbsList: List<BreadcrumbItem>,
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    onFileLongClick: (FileModel) -> Unit,
    onRestoreSelected: () -> Unit,
    onDeleteSelectedPermanently: () -> Unit,
    onEmptyTrash: () -> Unit,
    onSortSelected: (SortType, SortOrder) -> Unit,
    onViewModeSelected: (ViewMode) -> Unit
) {
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }

    val isPreview = LocalInspectionMode.current

    val hasContent = when (uiState) {
        is ExplorerUiState.Success -> uiState.files.isNotEmpty()
        else -> false
    }
    
    val shouldShowHeaders = hasContent || selectedFiles.isNotEmpty()

    if (!isPreview) {
        if (showSortSheet && hasContent) {
            SortBottomSheet(
                currentType = sortType,
                currentOrder = sortOrder,
                onDismiss = { showSortSheet = false },
                onSortSelected = { type, order ->
                    onSortSelected(type, order)
                    showSortSheet = false
                }
            )
        }

        if (showViewOptionsSheet && hasContent) {
            ViewOptionsBottomSheet(
                currentMode = viewMode,
                onDismiss = { showViewOptionsSheet = false },
                onModeSelected = { mode ->
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
                            text = if (selectedFiles.isNotEmpty()) "${selectedFiles.size} SELECTED" else "RECYCLE BIN",
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
                            IconButton(onClick = onRestoreSelected) {
                                Icon(painter = painterResource(id = R.drawable.reload_sync_icon),
                                    contentDescription = "Restore",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onDeleteSelectedPermanently) {
                                Icon(painter = painterResource(id = R.drawable.recycle_bin_line_icon),
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = "Delete Permanently",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            IconButton(onClick = onEmptyTrash) {
                                Icon( painter = painterResource(id = R.drawable.recycle_bin_line_icon),
                                    contentDescription = "Empty Trash",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
                is ExplorerUiState.Success -> {
                    val filteredFiles = if (searchQuery.isBlank()) uiState.files
                    else uiState.files.filter { it.name.contains(searchQuery, ignoreCase = true) }

                    if (filteredFiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.recycle_bin_line_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(65.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Recycle Bin is Empty",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Any deleted files and folders will show up here.",
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
                            Breadcrumbs(
                                items = breadcrumbsList,
                                onItemClick = { }
                            )
                            
                            FileList(
                                files = filteredFiles,
                                selectedFiles = selectedFiles,
                                viewMode = viewMode,
                                onFileClick = onFileClick,
                                onFileLongClick = onFileLongClick,
                                onDeleteClick = { onFileLongClick(it) },
                                onRenameClick = { },
                                onShareClick = { },
                                onOpenWithClick = { },
                                onFavoriteClick = { },
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
fun TrashScreenPreview() {
    val mockFiles = listOf(
        FileModel("Document_1.pdf", "/Trash/Doc1.pdf", 500L, System.currentTimeMillis(), false, "pdf"),
        FileModel("Song.mp3", "/Trash/Song.mp3", 4000L, System.currentTimeMillis(), false, "mp3"),
        FileModel("Photo.png", "/Trash/Photo.png", 200L, System.currentTimeMillis(), false, "png")
    )
    val mockBreadcrumbs = listOf(
        BreadcrumbItem("Internal Storage", "/root", null),
        BreadcrumbItem("Recycle Bin", "", null)
    )
    MaterialTheme {
        TrashContent(
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
            onRestoreSelected = {},
            onDeleteSelectedPermanently = {},
            onEmptyTrash = {},
            onSortSelected = { _, _ -> },
            onViewModeSelected = {}
        )
    }
}
