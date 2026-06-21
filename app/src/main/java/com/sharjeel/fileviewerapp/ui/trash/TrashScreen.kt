package com.sharjeel.fileviewerapp.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.explorer.*
import com.sharjeel.fileviewerapp.ui.theme.NeonSecondary

@OptIn(ExperimentalMaterial3Api::class)
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

    var showMenu by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (showSortSheet) {
        SortBottomSheet(
            currentType = sortType,
            currentOrder = sortOrder,
            onDismiss = { showSortSheet = false },
            onSortSelected = { type, order ->
                viewModel.updateSort(type, order)
                showSortSheet = false
            }
        )
    }

    if (showViewOptionsSheet) {
        ViewOptionsBottomSheet(
            currentMode = viewMode,
            onDismiss = { showViewOptionsSheet = false },
            onModeSelected = { mode ->
                viewModel.updateViewMode(mode)
                showViewOptionsSheet = false
            }
        )
    }

    BackHandler(enabled = selectedFiles.isNotEmpty() || isSearchActive) {
        if (selectedFiles.isNotEmpty()) {
            viewModel.clearSelection()
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            val bottomPad = innerPadding.calculateBottomPadding()
            when (val state = uiState) {
                is ExplorerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonSecondary)
                    }
                }
                is ExplorerUiState.Success -> {
                    val filteredFiles = if (searchQuery.isBlank()) state.files 
                                       else state.files.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    
                    if (filteredFiles.isEmpty() && !isSearchActive) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No files in trash", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        FileList(
                            title = "Recycle Bin",
                            currentPath = "Trash",
                            files = filteredFiles,
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
                            onSearchQueryChange = { searchQuery = it },
                            onRefreshClick = { viewModel.loadTrashFiles() },
                            onSelectAllClick = { viewModel.selectAll() },
                            onDeleteSelectedClick = { viewModel.deleteSelectedPermanently() },
                            onFileClick = { file ->
                                if (selectedFiles.isNotEmpty()) {
                                    viewModel.toggleFileSelection(file.path)
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onFileLongClick = { viewModel.toggleFileSelection(it.path) },
                            onDeleteClick = { viewModel.toggleFileSelection(it.path); viewModel.deleteSelectedPermanently() },
                            onRenameClick = { },
                            onShareClick = { },
                            onOpenWithClick = { },
                            onFavoriteClick = { },
                            onExtractClick = { },
                            onLockClick = { },
                            onPathClick = { },
                            onMoveClick = { },
                            onCopyClick = { },
                            bottomPadding = bottomPad,
                            onRestoreSelectedClick = { viewModel.restoreSelected() },
                            onRestoreAllClick = { viewModel.restoreAll() },
                            isEmptyTrashEnabled = true,
                            onEmptyTrashClick = { viewModel.emptyTrash() },
                            // Header and Breadcrumbs will show when content exists
                        )
                    }
                }
                is ExplorerUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
