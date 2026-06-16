package com.sharjeel.fileviewerapp.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState
import com.sharjeel.fileviewerapp.ui.explorer.FileList
import com.sharjeel.fileviewerapp.ui.explorer.RenameDialog
import com.sharjeel.fileviewerapp.ui.explorer.SearchTopBar
import com.sharjeel.fileviewerapp.ui.theme.GlassSurface
import com.sharjeel.fileviewerapp.ui.theme.NeonPrimary
import com.sharjeel.fileviewerapp.ui.theme.NeonSecondary
import com.sharjeel.fileviewerapp.util.BiometricHelper
import com.sharjeel.fileviewerapp.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onBackClick: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fileToRename by remember { mutableStateOf<FileModel?>(null) }
    val isDark = isSystemInDarkTheme()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                if (isSearchActive && isUnlocked) {
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
                                "SECURE VAULT", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack, 
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            if (isUnlocked) {
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
                                                viewModel.unlock() // Reuse unlock to reload
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null, tint = NeonPrimary) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Lock Vault") },
                                            onClick = { 
                                                showMenu = false 
                                                viewModel.lock()
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = NeonPrimary) }
                                        )
                                    }
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
                        color = if (isDark) GlassSurface else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPrimary.copy(alpha = 0.3f)),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = NeonPrimary
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
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPrimary),
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
                            CircularProgressIndicator(color = NeonSecondary)
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
                                        if (searchQuery.isNotEmpty()) Icons.Rounded.Refresh else Icons.Rounded.Lock,
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
                                currentPath = "vault",
                                files = filteredFiles,
                                selectedFiles = emptySet(),
                                onFileClick = { FileUtils.openWithExternalApp(context, it.path) },
                                onFileLongClick = { },
                                onDeleteClick = { viewModel.deleteFile(it.path) },
                                onRenameClick = { fileToRename = it },
                                onShareClick = { FileUtils.shareFile(context, it.path) },
                                onOpenWithClick = { FileUtils.openWithExternalApp(context, it.path) },
                                onFavoriteClick = { },
                                onExtractClick = { },
                                onLockClick = { viewModel.removeFromVault(it) },
                                onPathClick = { },
                                onMoveClick = { },
                                onCopyClick = { },
                                bottomPadding = innerPadding.calculateBottomPadding()
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
