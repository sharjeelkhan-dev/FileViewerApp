package com.sharjeel.fileviewerapp.ui

import android.os.Environment
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.repository.FileCategory
import com.sharjeel.fileviewerapp.ui.components.AppScaffold
import com.sharjeel.fileviewerapp.ui.explorer.ExplorerScreen
import com.sharjeel.fileviewerapp.ui.explorer.ExplorerViewModel
import com.sharjeel.fileviewerapp.ui.favorites.FavoritesScreen
import com.sharjeel.fileviewerapp.ui.home.HomeScreen
import com.sharjeel.fileviewerapp.ui.navigation.NavRoute
import com.sharjeel.fileviewerapp.ui.settings.SettingsScreen
import com.sharjeel.fileviewerapp.ui.trash.TrashScreen
import com.sharjeel.fileviewerapp.ui.vault.VaultScreen
import com.sharjeel.fileviewerapp.ui.viewer.FileViewerScreen
import com.sharjeel.fileviewerapp.ui.viewer.ViewerViewModel
import com.sharjeel.fileviewerapp.util.PermissionHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(initialRoute: NavRoute = NavRoute.Home) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val backstack = rememberSaveable(
        saver = listSaver<SnapshotStateList<NavRoute>, String>(
            save = { it.toList().map { route -> Json.encodeToString(route) } },
            restore = { it.map { json -> Json.decodeFromString<NavRoute>(json) }.toMutableStateList() }
        )
    ) {
        val list = mutableStateListOf<NavRoute>(NavRoute.Home)
        if (initialRoute != NavRoute.Home) {
            list.add(initialRoute)
        }
        list
    }

    LaunchedEffect(initialRoute) {
        if (initialRoute != NavRoute.Home && initialRoute != backstack.lastOrNull()) {
            backstack.add(initialRoute)
        }
    }
    val currentRoute = backstack.last()

    val explorerViewModel: ExplorerViewModel = hiltViewModel()
    val viewerViewModel: ViewerViewModel = hiltViewModel()
    val trashViewModel: com.sharjeel.fileviewerapp.ui.trash.TrashViewModel = hiltViewModel()
    val vaultViewModel: com.sharjeel.fileviewerapp.ui.vault.VaultViewModel = hiltViewModel()
    val aiViewModel: com.sharjeel.fileviewerapp.ui.ai.AIViewModel = hiltViewModel()

    val explorerFiles by explorerViewModel.uiState.collectAsState()
    val trashFiles by trashViewModel.uiState.collectAsState()
    val vaultFiles by vaultViewModel.uiState.collectAsState()
    val aiUiState by aiViewModel.uiState.collectAsState()

    var showAIPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(aiUiState) {
        if (aiUiState is com.sharjeel.fileviewerapp.ui.ai.AIUiState.AppAction) {
            val action = (aiUiState as com.sharjeel.fileviewerapp.ui.ai.AIUiState.AppAction).action
            when {
                action.startsWith("NAVIGATE:") -> {
                    val destination = action.removePrefix("NAVIGATE:")
                    backstack.clear()
                    backstack.add(NavRoute.Home)
                    when (destination) {
                        "HOME" -> { /* Already cleared to home */ }
                        "STORAGE" -> backstack.add(NavRoute.Explorer(title = "Storage", path = Environment.getExternalStorageDirectory().absolutePath))
                        "DOWNLOADS" -> backstack.add(NavRoute.Explorer(title = "Downloads"))
                        "RECENT" -> backstack.add(NavRoute.Explorer(title = "Recent"))
                        "FAVORITES" -> backstack.add(NavRoute.Favorites)
                        "VAULT" -> backstack.add(NavRoute.Vault)
                        "TRASH" -> backstack.add(NavRoute.Trash)
                        "SETTINGS" -> backstack.add(NavRoute.Settings)
                    }
                }
                action.startsWith("SEARCH:") -> {
                    val query = action.removePrefix("SEARCH:")
                    backstack.clear()
                    backstack.add(NavRoute.Home)
                    backstack.add(NavRoute.Explorer(title = "Search Results"))
                    explorerViewModel.setSearchQuery(query)
                }
            }
            aiViewModel.resetState()
        }
    }

    PermissionHandler {
        // Permissions granted
    }

    val navSuiteType = NavigationSuiteType.None

    NavigationSuiteScaffold(
        layoutType = navSuiteType,
        modifier = Modifier.fillMaxSize(),
        navigationSuiteItems = {
            // Managed by Drawer
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    DrawerHeader()
                    Spacer(modifier = Modifier.height(12.dp))

                    NavigationDrawerItem(
                        label = { Text("Home") },
                        selected = currentRoute == NavRoute.Home,
                        onClick = {
                            if (currentRoute != NavRoute.Home) {
                                backstack.clear()
                                backstack.add(NavRoute.Home)
                            }
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.house_window_icon),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Internal Storage") },
                        selected = currentRoute is NavRoute.Explorer && (currentRoute as NavRoute.Explorer).title == "Storage",
                        onClick = {
                            backstack.clear()
                            backstack.add(NavRoute.Home)
                            val path = Environment.getExternalStorageDirectory().absolutePath
                            backstack.add(NavRoute.Explorer(title = "Storage", path = path))
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.database_line_icon),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Downloads") },
                        selected = currentRoute is NavRoute.Explorer && (currentRoute as NavRoute.Explorer).title == "Downloads",
                        onClick = {
                            backstack.clear()
                            backstack.add(NavRoute.Home)
                            backstack.add(NavRoute.Explorer(title = "Downloads"))
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.import_icon),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Recent Files") },
                        selected = currentRoute is NavRoute.Explorer && (currentRoute as NavRoute.Explorer).title == "Recent",
                        onClick = {
                            backstack.clear()
                            backstack.add(NavRoute.Home)
                            backstack.add(NavRoute.Explorer(title = "Recent"))
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.rotate_left_arrow_icon),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Favorites") },
                        selected = currentRoute == NavRoute.Favorites,
                        onClick = {
                            backstack.clear()
                            backstack.add(NavRoute.Home)
                            backstack.add(NavRoute.Favorites)
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.photo_collage_icon),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Recycle Bin") },
                        selected = currentRoute == NavRoute.Trash,
                        onClick = {
                            backstack.clear()
                            backstack.add(NavRoute.Home)
                            backstack.add(NavRoute.Trash)
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.recycle_bin_line_icon),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )

                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = currentRoute == NavRoute.Settings,
                        onClick = {
                            backstack.clear()
                            backstack.add(NavRoute.Home)
                            backstack.add(NavRoute.Settings)
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        ) {
            if (showAIPrompt) {
                BasicAlertDialog(
                    onDismissRequest = {
                        showAIPrompt = false
                        aiViewModel.resetState()
                    },
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Gemini Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            var userPrompt by remember { mutableStateOf("") }

                            OutlinedTextField(
                                value = userPrompt,
                                onValueChange = { userPrompt = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("How can I help you today?") },
                                shape = RoundedCornerShape(12.dp),
                                enabled = aiUiState !is com.sharjeel.fileviewerapp.ui.ai.AIUiState.Loading
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            if (aiUiState is com.sharjeel.fileviewerapp.ui.ai.AIUiState.Loading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = (aiUiState as com.sharjeel.fileviewerapp.ui.ai.AIUiState.Loading).message,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (aiUiState is com.sharjeel.fileviewerapp.ui.ai.AIUiState.Error) {
                                Text(
                                    text = (aiUiState as com.sharjeel.fileviewerapp.ui.ai.AIUiState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    showAIPrompt = false
                                    aiViewModel.resetState()
                                }) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        if (userPrompt.isNotBlank()) {
                                            aiViewModel.executeGlobalCommand(userPrompt)
                                        }
                                    },
                                    enabled = userPrompt.isNotBlank() && aiUiState !is com.sharjeel.fileviewerapp.ui.ai.AIUiState.Loading,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Execute")
                                }
                            }
                        }
                    }
                }
            }

            AppScaffold(
                containerColor = MaterialTheme.colorScheme.background
            ) { _ ->
                Box(modifier = Modifier.fillMaxSize()) {
                    NavDisplay(
                        backStack = backstack,
                        onBack = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) },
                        transitionSpec = {
                            (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                                    (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                        },
                        popTransitionSpec = {
                            (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
                                    (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                        },
                        predictivePopTransitionSpec = {
                            (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
                                    (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                        }
                    ) { route ->
                        when (route) {
                            is NavRoute.Home -> NavEntry(route) {
                                HomeScreen(
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    onAIClick = { showAIPrompt = true },
                                    onStorageClick = {
                                        val path = Environment.getExternalStorageDirectory().absolutePath
                                        backstack.add(NavRoute.Explorer(title = "Storage", path = path))
                                    },
                                    onCategoryClick = { categoryName ->
                                        backstack.add(NavRoute.Explorer(title = categoryName))
                                    },
                                    onPlaceClick = { placeName ->
                                        when (placeName) {
                                            "Downloads" -> backstack.add(NavRoute.Explorer(title = "Downloads"))
                                            "Recent" -> backstack.add(NavRoute.Explorer(title = "Recent"))
                                            "Favorites" -> backstack.add(NavRoute.Favorites)
                                            "Vault" -> backstack.add(NavRoute.Vault)
                                            "Trash" -> backstack.add(NavRoute.Trash)
                                        }
                                    }
                                )
                            }
                            is NavRoute.Explorer -> NavEntry(route) {
                                val key = route as NavRoute.Explorer

                                LaunchedEffect(key.title, key.path) {
                                    if (key.path != null) {
                                        explorerViewModel.loadFiles(key.path!!)
                                    } else {
                                        when (key.title) {
                                            "Storage" -> explorerViewModel.loadFiles(Environment.getExternalStorageDirectory().absolutePath)
                                            "Downloads" -> explorerViewModel.loadCategory(FileCategory.DOWNLOADS)
                                            "Recent" -> explorerViewModel.loadRecent()
                                            "Images" -> explorerViewModel.loadCategory(FileCategory.IMAGES)
                                            "Videos" -> explorerViewModel.loadCategory(FileCategory.VIDEOS)
                                            "Audio" -> explorerViewModel.loadCategory(FileCategory.AUDIO)
                                            "Docs" -> explorerViewModel.loadCategory(FileCategory.DOCUMENTS)
                                            "Archives" -> explorerViewModel.loadCategory(FileCategory.ARCHIVES)
                                            else -> {}
                                        }
                                    }
                                }

                                ExplorerScreen(
                                    title = key.title,
                                    viewModel = explorerViewModel,
                                    onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) },
                                    onFileClick = { file ->
                                        if (file.isDirectory) {
                                            backstack.add(NavRoute.Explorer(title = file.name, path = file.path))
                                            explorerViewModel.loadFiles(file.path)
                                        } else {
                                            if (explorerFiles is com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState.Success) {
                                                viewerViewModel.setPlaylist(
                                                    (explorerFiles as com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState.Success).files,
                                                    file.path
                                                )
                                            }
                                            backstack.add(NavRoute.Viewer(file.path, file.extension))
                                        }
                                    },
                                    onPathClick = { path ->
                                        if (path == "CATEGORY_ROOT") {
                                            if (backstack.size > 1) {
                                                backstack.removeAt(backstack.lastIndex)
                                                val previousPath = (backstack.last() as? NavRoute.Explorer)?.path ?: ""
                                                explorerViewModel.loadFiles(previousPath)
                                            }
                                        } else {
                                            backstack.add(NavRoute.Explorer(title = key.title, path = path))
                                            explorerViewModel.loadFiles(path)
                                        }
                                    },
                                    onHomeClick = {
                                        // Synchronously reset backstack clean directly to NavRoute.Home
                                        backstack.clear()
                                        backstack.add(NavRoute.Home)
                                    }
                                )
                            }
                            is NavRoute.Viewer -> NavEntry(route) {
                                val key = route as NavRoute.Viewer
                                FileViewerScreen(
                                    filePath = key.filePath,
                                    fileType = key.fileType,
                                    viewModel = viewerViewModel,
                                    onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) },
                                    onShowInFolder = { folderPath ->
                                        if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
                                        backstack.add(NavRoute.Explorer(title = java.io.File(folderPath).name, path = folderPath))
                                    }
                                )
                            }
                            is NavRoute.Vault -> NavEntry(route) {
                                VaultScreen(
                                    viewModel = vaultViewModel,
                                    onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) },
                                    onFileClick = { file ->
                                        val state = vaultFiles
                                        if (state is com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState.Success) {
                                            viewerViewModel.setPlaylist(state.files, file.path)
                                        }
                                        backstack.add(NavRoute.Viewer(file.path, file.extension))
                                    }
                                )
                            }
                            is NavRoute.Trash -> NavEntry(route) {
                                TrashScreen(
                                    viewModel = trashViewModel,
                                    onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) },
                                    onFileClick = { file ->
                                        val state = trashFiles
                                        if (state is com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState.Success) {
                                            viewerViewModel.setPlaylist(state.files, file.path)
                                        }
                                        backstack.add(NavRoute.Viewer(file.path, file.extension))
                                    }
                                )
                            }
                            is NavRoute.Favorites -> NavEntry(route) {
                                val favoritesViewModel: com.sharjeel.fileviewerapp.ui.favorites.FavoritesViewModel = hiltViewModel()
                                val favoritesFiles by favoritesViewModel.uiState.collectAsState()

                                FavoritesScreen(
                                    viewModel = favoritesViewModel,
                                    onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) },
                                    onFileClick = { file ->
                                        if (favoritesFiles is com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState.Success) {
                                            viewerViewModel.setPlaylist((favoritesFiles as com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState.Success).files, file.path)
                                        }
                                        backstack.add(NavRoute.Viewer(file.path, file.extension))
                                    }
                                )
                            }
                            is NavRoute.Settings -> NavEntry(route) {
                                SettingsScreen(onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerHeader() {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Text(
            text = "FILE VIEWER",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}