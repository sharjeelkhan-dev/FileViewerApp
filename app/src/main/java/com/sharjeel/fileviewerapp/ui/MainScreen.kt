package com.sharjeel.fileviewerapp.ui

import android.os.Environment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.sharjeel.fileviewerapp.R
import com.sharjeel.fileviewerapp.domain.repository.FileCategory
import com.sharjeel.fileviewerapp.ui.explorer.ExplorerScreen
import com.sharjeel.fileviewerapp.ui.explorer.ExplorerViewModel
import com.sharjeel.fileviewerapp.ui.home.HomeScreen
import com.sharjeel.fileviewerapp.ui.navigation.NavRoute
import com.sharjeel.fileviewerapp.ui.settings.SettingsScreen
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import com.sharjeel.fileviewerapp.ui.theme.GlassSurface
import com.sharjeel.fileviewerapp.ui.theme.NeonSecondary
import com.sharjeel.fileviewerapp.ui.vault.VaultScreen
import com.sharjeel.fileviewerapp.ui.viewer.FileViewerScreen
import com.sharjeel.fileviewerapp.ui.viewer.ViewerViewModel
import com.sharjeel.fileviewerapp.util.PermissionHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    
    val backstack = rememberSaveable(
        saver = listSaver<SnapshotStateList<NavRoute>, String>(
            save = { it.toList().map { route -> Json.encodeToString(route) } },
            restore = { it.map { json -> Json.decodeFromString<NavRoute>(json) }.toMutableStateList() }
        )
    ) { mutableStateListOf<NavRoute>(NavRoute.Home) }
    val currentRoute = backstack.last()

    val explorerViewModel: ExplorerViewModel = hiltViewModel()
    val viewerViewModel: ViewerViewModel = hiltViewModel()

    PermissionHandler {
        // Permissions granted
    }

    val navSuiteType = if (adaptiveInfo.windowSizeClass.windowWidthSizeClass == androidx.window.core.layout.WindowWidthSizeClass.EXPANDED) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.None
    }

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
                    windowInsets = WindowInsets.systemBars
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
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = NeonSecondary,
                            selectedIconColor = NeonSecondary
                        )
                    )
                    
                    NavigationDrawerItem(
                        label = { Text("Internal Storage") },
                        selected = currentRoute is NavRoute.Explorer && (currentRoute as NavRoute.Explorer).title == "Storage",
                        onClick = {
                            explorerViewModel.loadFiles(Environment.getExternalStorageDirectory().absolutePath)
                            backstack.add(NavRoute.Explorer(title = "Storage"))
                            scope.launch { drawerState.close() }
                        },
                        icon = { 
                            Icon(
                                painter = painterResource(R.drawable.database_line_icon), 
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = NeonSecondary,
                            selectedIconColor = NeonSecondary
                        )
                    )
                    
                    NavigationDrawerItem(
                        label = { Text("Downloads") },
                        selected = currentRoute is NavRoute.Explorer && (currentRoute as NavRoute.Explorer).title == "Downloads",
                        onClick = { 
                            explorerViewModel.loadCategory(FileCategory.DOWNLOADS)
                            backstack.add(NavRoute.Explorer(title = "Downloads"))
                            scope.launch { drawerState.close() }
                        },
                        icon = { 
                            Icon(
                                painter = painterResource(R.drawable.import_icon), 
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = NeonSecondary,
                            selectedIconColor = NeonSecondary
                        )
                    )
                    
                    NavigationDrawerItem(
                        label = { Text("Recent Files") },
                        selected = currentRoute is NavRoute.Explorer && (currentRoute as NavRoute.Explorer).title == "Recent",
                        onClick = { 
                            explorerViewModel.loadRecent()
                            backstack.add(NavRoute.Explorer(title = "Recent"))
                            scope.launch { drawerState.close() }
                        },
                        icon = { 
                            Icon(
                                painter = painterResource(R.drawable.rotate_left_arrow_icon), 
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = NeonSecondary,
                            selectedIconColor = NeonSecondary
                        )
                    )
                    
                    NavigationDrawerItem(
                        label = { Text("Favorites") },
                        selected = currentRoute is NavRoute.Explorer && (currentRoute as NavRoute.Explorer).title == "Favorites",
                        onClick = { 
                            explorerViewModel.loadFavorites()
                            backstack.add(NavRoute.Explorer(title = "Favorites"))
                            scope.launch { drawerState.close() }
                        },
                        icon = { 
                            Icon(
                                painter = painterResource(R.drawable.photo_collage_icon), 
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = NeonSecondary,
                            selectedIconColor = NeonSecondary
                        )
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = currentRoute == NavRoute.Settings,
                        onClick = { 
                            backstack.add(NavRoute.Settings)
                            scope.launch { drawerState.close() }
                        },
                        icon = { 
                            Icon(
                                Icons.Rounded.Settings, 
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = NeonSecondary,
                            selectedIconColor = NeonSecondary
                        )
                    )
                }
            }
        )
        {
            NavDisplay(
                backStack = backstack,
                onBack = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) }
            ) { route ->
                when (route) {
                    is NavRoute.Home -> NavEntry(route) {
                        HomeScreen(
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onStorageClick = {
                                explorerViewModel.loadFiles(Environment.getExternalStorageDirectory().absolutePath)
                                backstack.add(NavRoute.Explorer(title = "Storage"))
                            },
                            onCategoryClick = { categoryName ->
                                val category = when (categoryName) {
                                    "Images" -> FileCategory.IMAGES
                                    "Videos" -> FileCategory.VIDEOS
                                    "Audio" -> FileCategory.AUDIO
                                    "Docs" -> FileCategory.DOCUMENTS
                                    "Archives" -> FileCategory.ARCHIVES
                                    else -> FileCategory.DOCUMENTS
                                }
                                explorerViewModel.loadCategory(category)
                                backstack.add(NavRoute.Explorer(title = categoryName))
                            },
                            onPlaceClick = { placeName ->
                                when (placeName) {
                                    "Downloads" -> {
                                        explorerViewModel.loadCategory(FileCategory.DOWNLOADS)
                                        backstack.add(NavRoute.Explorer(title = "Downloads"))
                                    }
                                    "Recent" -> explorerViewModel.loadRecent().also { backstack.add(NavRoute.Explorer(title = "Recent")) }
                                    "Favorites" -> explorerViewModel.loadFavorites().also { backstack.add(NavRoute.Explorer(title = "Favorites")) }
                                    "Vault" -> backstack.add(NavRoute.Vault)
                                }
                            },
                            onSearchClick = {
                                explorerViewModel.loadFiles(Environment.getExternalStorageDirectory().absolutePath)
                                backstack.add(NavRoute.Explorer(title = "Search"))
                            },
                            onRefresh = { explorerViewModel.refresh() },
                            onSelectAll = { /* TODO */ }
                        )
                    }
                    is NavRoute.Explorer -> NavEntry(route) {
                        val key = route as NavRoute.Explorer
                        ExplorerScreen(
                            title = key.title,
                            viewModel = explorerViewModel,
                            onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) },
                            onFileClick = { file ->
                                if (file.isDirectory) {
                                    explorerViewModel.loadFiles(file.path)
                                } else {
                                    backstack.add(NavRoute.Viewer(file.path, file.extension))
                                }
                            }
                        )
                    }
                    is NavRoute.Viewer -> NavEntry(route) {
                        val key = route as NavRoute.Viewer
                        FileViewerScreen(
                            filePath = key.filePath,
                            fileType = key.fileType,
                            viewModel = viewerViewModel,
                            onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) }
                        )
                    }
                    is NavRoute.Vault -> NavEntry(route) {
                        VaultScreen(onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) })
                    }
                    is NavRoute.Settings -> NavEntry(route) {
                        SettingsScreen(onBackClick = { if (backstack.size > 1) backstack.removeAt(backstack.lastIndex) })
                    }
                    else -> NavEntry(route) {
                        Box(modifier = Modifier.fillMaxSize()) { Text("Other Screen", color = MaterialTheme.colorScheme.onBackground) }
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
            style = MaterialTheme.typography.headlineSmall,
            color = NeonSecondary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Preview(showBackground = true, name = "Drawer Light Mode")
@Composable
fun DrawerPreviewLight() {
    FileViewerAppTheme(darkTheme = false) {
        ModalDrawerSheet(
            drawerContainerColor = MaterialTheme.colorScheme.surface,
            drawerContentColor = MaterialTheme.colorScheme.onSurface
        ) {
            DrawerHeader()
            Spacer(modifier = Modifier.height(12.dp))
            NavigationDrawerItem(
                label = { Text("Home") },
                selected = true,
                onClick = {},
                icon = { Icon(painter = painterResource
                    (id = R.drawable.house_window_icon),
                    contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults
                    .ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text("Internal Storage") },
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Rounded.SdStorage,
                    contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults
                    .ItemPadding)
            )
            HorizontalDivider(modifier = Modifier
                .padding(vertical = 8.dp,
                    horizontal = 28.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
}

@Preview(showBackground = true, name = "Drawer Dark Mode")
@Composable
fun DrawerPreviewDark() {
    FileViewerAppTheme(darkTheme = true) {
        ModalDrawerSheet(
            drawerContainerColor = GlassSurface,
            drawerContentColor = Color.White
        ) {
            DrawerHeader()
            Spacer(modifier = Modifier.height(12.dp))
            NavigationDrawerItem(
                label = { Text("Home") },
                selected = true,
                onClick = {},
                icon = { 
                    Icon(
                        painter = painterResource(R.drawable.house_window_icon), 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    ) 
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedTextColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                    selectedTextColor = NeonSecondary,
                    selectedIconColor = NeonSecondary
                )
            )
        }
    }
}
