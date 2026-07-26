package com.sharjeel.fileviewerapp.ui.vault
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.components.AppScaffold
import com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState
import com.sharjeel.fileviewerapp.ui.explorer.FileList
import com.sharjeel.fileviewerapp.ui.explorer.FileThumbnail
import com.sharjeel.fileviewerapp.ui.explorer.RenameDialog
import com.sharjeel.fileviewerapp.ui.explorer.SearchTopBar
import com.sharjeel.fileviewerapp.ui.explorer.SortBottomSheet
import com.sharjeel.fileviewerapp.ui.explorer.ViewOptionsBottomSheet
import com.sharjeel.fileviewerapp.util.BiometricHelper
import com.sharjeel.fileviewerapp.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onBackClick: () -> Unit,
    onFileClick: (FileModel) -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val breadcrumbsList by viewModel.breadcrumbs.collectAsState()

    val aiViewModel: com.sharjeel.fileviewerapp.ui.ai.AIViewModel = hiltViewModel()
    val aiUiState by aiViewModel.uiState.collectAsState()

    // Handled AI suggestion state to safely bypass unresolved reference compilation errors
    LaunchedEffect(aiUiState) {
        if (aiUiState is com.sharjeel.fileviewerapp.ui.ai.AIUiState.NamingSuggestion) {
            val suggestion = aiUiState as com.sharjeel.fileviewerapp.ui.ai.AIUiState.NamingSuggestion
            viewModel.renameFile(suggestion.filePath, suggestion.name)
            aiViewModel.resetState()
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fileToRename by remember { mutableStateOf<FileModel?>(null) }
    var fileForActions by remember { mutableStateOf<FileModel?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val isSelectionActive = selectedFiles.isNotEmpty()

    BackHandler(enabled = isSelectionActive || isSearchActive) {
        if (isSelectionActive) {
            viewModel.clearSelection()
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

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

    if (fileForActions != null) {
        VaultFileActionBottomSheet(
            file = fileForActions!!,
            onDismiss = { fileForActions = null },
            onRenameClick = { fileToRename = it },
            onUnlockClick = { viewModel.removeFromVault(it) },
            onDeleteClick = { viewModel.deleteFile(it.path) },
            onShareClick = { FileUtils.shareFile(context, it.path) },
            onSelectClick = { viewModel.toggleFileSelection(it.path) }
        )
    }

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

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isUnlocked) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    if (isSearchActive) {
                        SearchTopBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onCloseClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }
                        )
                    } else {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (isSelectionActive) "${selectedFiles.size} items" else "Vault",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    if (isSelectionActive) {
                                        viewModel.clearSelection()
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
                                        val filesList = (uiState as ExplorerUiState.Success).files
                                        val isAllSelected = selectedFiles.size == filesList.size && filesList.isNotEmpty()
                                        IconButton(onClick = {
                                            if (isAllSelected) viewModel.clearSelection() else viewModel.selectAll()
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
                                                text = { Text("Lock Vault") },
                                                onClick = {
                                                    showMenu = false
                                                    viewModel.lock()
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.shield_lock_line_icon),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
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
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionActive && isUnlocked,
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
                            drawableRes = R.drawable.lock_line_icon,
                            label = "Unlock",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { viewModel.removeSelectedFromVault() }
                        )
                        SelectionActionButton(
                            drawableRes = R.drawable.share_icon,
                            label = "Share",
                            onClick = {
                                if (selectedFiles.size == 1) {
                                    FileUtils.shareFile(context, selectedFiles.first())
                                }
                            }
                        )
                        SelectionActionButton(
                            drawableRes = R.drawable.delete_icon,
                            label = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { viewModel.deleteSelectedFiles() }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (!isUnlocked) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.shield_lock_line_icon),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        "Vault is Locked",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Please authenticate to access your private and encrypted files.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            activity?.let {
                                BiometricHelper.showBiometricPrompt(
                                    activity = it,
                                    title = "Vault Access",
                                    subtitle = "Authenticate to open the vault",
                                    onSuccess = { viewModel.unlock() },
                                    onError = { error -> errorMessage = error }
                                )
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth(0.8f),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            "UNLOCK WITH BIOMETRICS",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                when (val state = uiState) {
                    is ExplorerUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    is ExplorerUiState.Success -> {
                        val filteredFiles = if (searchQuery.isBlank()) {
                            state.files
                        } else {
                            state.files.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }

                        if (filteredFiles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = if (searchQuery.isNotEmpty()) painterResource(id = R.drawable.reload_sync_icon) else painterResource(id = R.drawable.shield_lock_line_icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        if (searchQuery.isNotEmpty()) "No results found" else "Your vault is empty",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (searchQuery.isEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Add files from the explorer by selecting 'Move to Vault'",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 48.dp)
                                        )
                                    }
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
                                onFileClick = { file ->
                                    if (isSelectionActive) {
                                        viewModel.toggleFileSelection(file.path)
                                    } else {
                                        onFileClick(file)
                                    }
                                },
                                onFileLongClick = { viewModel.toggleFileSelection(it.path) },
                                onFileActionsClick = { fileForActions = it },
                                onBreadcrumbClick = { },
                                onSortClick = { showSortSheet = true },
                                onViewModeClick = { showViewOptionsSheet = true },
                                onSelectionToggle = { viewModel.toggleFileSelection(it.path) },
                                bottomPadding = paddingValues.calculateBottomPadding()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultFileActionBottomSheet(
    file: FileModel,
    onDismiss: () -> Unit,
    onRenameClick: (FileModel) -> Unit,
    onUnlockClick: (FileModel) -> Unit,
    onDeleteClick: (FileModel) -> Unit,
    onShareClick: (FileModel) -> Unit,
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

            FileActionItem(drawableRes = R.drawable.lock_line_icon, label = "Unlock", tint = MaterialTheme.colorScheme.primary) { onUnlockClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.share_icon, label = "Share") { onShareClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.rename_icon, label = "Rename") { onRenameClick(file); onDismiss() }
            FileActionItem(drawableRes = R.drawable.approve_accept_icon, label = "Select") { onSelectClick(file); onDismiss() }

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
