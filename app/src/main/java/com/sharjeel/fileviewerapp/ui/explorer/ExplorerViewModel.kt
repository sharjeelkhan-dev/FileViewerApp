package com.sharjeel.fileviewerapp.ui.explorer

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.domain.repository.FileCategory
import com.sharjeel.fileviewerapp.domain.repository.FileRepository
import com.sharjeel.fileviewerapp.util.AIService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- UI STATE SEALED INTERFACE ---
@Immutable
sealed interface ExplorerUiState {
    object Loading : ExplorerUiState

    @Immutable
    data class Success(val files: List<FileModel>) : ExplorerUiState

    @Immutable
    data class Error(val message: String) : ExplorerUiState
}

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val repository: FileRepository,
    private val aiService: AIService,
    @ApplicationContext private val appContext: Context // ✅ Memory Leak Fix via Hilt
) : ViewModel() {

    private val _events = Channel<ExplorerEvent>(Channel.CONFLATED)
    val events = _events.receiveAsFlow()

    private val _rawFiles = MutableStateFlow<List<FileModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.NAME)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.SMALL)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _pickingFolderForArchive = MutableStateFlow<FileModel?>(null)
    val pickingFolderForArchive: StateFlow<FileModel?> = _pickingFolderForArchive.asStateFlow()

    private val _isMoving = MutableStateFlow<List<String>>(emptyList())
    val isMoving = _isMoving.asStateFlow()

    private val _isCopying = MutableStateFlow<List<String>>(emptyList())
    val isCopying = _isCopying.asStateFlow()

    // ✅ FIXED: Heavy computation moved to Dispatchers.Default (Background Thread)
    val uiState: StateFlow<ExplorerUiState> = combine(
        _rawFiles,
        _searchQuery,
        _sortType,
        _sortOrder
    ) { files, query, sortType, sortOrder ->
        withContext(Dispatchers.Default) {
            try {
                val filtered = if (query.isBlank()) {
                    files
                } else {
                    files.filter { it.name.contains(query, ignoreCase = true) }
                }

                val isAsc = sortOrder == SortOrder.ASCENDING
                val sorted = when (sortType) {
                    SortType.NAME -> if (isAsc) filtered.sortedBy { it.name.lowercase() } else filtered.sortedByDescending { it.name.lowercase() }
                    SortType.TYPE -> if (isAsc) filtered.sortedBy { it.extension.lowercase() } else filtered.sortedByDescending { it.extension.lowercase() }
                    SortType.SIZE -> if (isAsc) filtered.sortedBy { it.size } else filtered.sortedByDescending { it.size }
                    SortType.DATE -> if (isAsc) filtered.sortedBy { it.lastModified } else filtered.sortedByDescending { it.lastModified }
                }

                ExplorerUiState.Success(sorted)
            } catch (e: Exception) {
                e.printStackTrace()
                ExplorerUiState.Error(e.localizedMessage ?: "Unknown Error occurred")
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExplorerUiState.Loading
    )

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _currentCategory = MutableStateFlow<FileCategory?>(null)
    val currentCategory: StateFlow<FileCategory?> = _currentCategory.asStateFlow()

    // ✅ FIXED: Moved breadcrumbs String manipulation to Dispatchers.Default
    val breadcrumbs: StateFlow<List<BreadcrumbItem>> = combine(
        _currentPath,
        _currentCategory
    ) { path, category ->
        val items = ArrayList<BreadcrumbItem>()
        val storageRoot = Environment.getExternalStorageDirectory().absolutePath

        items.add(BreadcrumbItem(name = "Home", path = "", category = null))
        items.add(BreadcrumbItem(name = "Internal Storage", path = storageRoot, category = null))

        if (category != null) {
            val categoryLabel = when(category) {
                FileCategory.IMAGES -> "Images"
                FileCategory.VIDEOS -> "Videos"
                FileCategory.AUDIO -> "Audio"
                FileCategory.DOCUMENTS -> "Documents"
                FileCategory.ARCHIVES -> "Archives"
                FileCategory.DOWNLOADS -> "Downloads"
                FileCategory.RECENT -> "Recent"
                FileCategory.FAVORITES -> "Favorites"
            }
            items.add(BreadcrumbItem(name = categoryLabel, path = "", category = category))
        } else if (path.isNotEmpty()) {
            if (path.startsWith(storageRoot)) {
                if (path != storageRoot) {
                    val relativePath = path.removePrefix(storageRoot).trim('/')
                    if (relativePath.isNotEmpty()) {
                        val segments = relativePath.split('/')
                        var currentAccumulatedPath = storageRoot
                        segments.forEach { segment ->
                            currentAccumulatedPath = "$currentAccumulatedPath/$segment"
                            items.add(BreadcrumbItem(name = segment, path = currentAccumulatedPath, category = null))
                        }
                    }
                }
            } else {
                val file = File(path)
                items.add(BreadcrumbItem(name = file.name, path = path, category = null))
            }
        }
        items
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(BreadcrumbItem("Internal Storage", Environment.getExternalStorageDirectory().absolutePath, null))
        )

    fun setSort(type: SortType, order: SortOrder) {
        _sortType.value = type
        _sortOrder.value = order
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun resetToHome() {
        _currentPath.value = ""
        _currentCategory.value = null
        _rawFiles.value = emptyList()
        _searchQuery.value = ""
        clearSelection()
        _events.trySend(ExplorerEvent.NavigateToHome)
    }

    fun loadFiles(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentPath.value = path
            _currentCategory.value = null
            if (path.isEmpty()) {
                _rawFiles.value = emptyList()
            } else {
                repository.getFiles(File(path)).collect { files ->
                    _rawFiles.value = files
                }
            }
        }
    }

    fun navigateToDirectory(targetPath: String) {
        viewModelScope.launch {
            loadFiles(targetPath)
            clearSelection()
        }
    }

    fun handleBackNavigation(): Boolean {
        if (_selectedFiles.value.isNotEmpty()) {
            clearSelection()
            return true
        }

        if (isSearchActive()) {
            return false
        }

        val storageRoot = Environment.getExternalStorageDirectory().absolutePath
        val current = _currentPath.value

        if (_currentCategory.value != null) {
            resetToHome()
            return true
        }

        if (current.isNotEmpty()) {
            if (current == storageRoot) {
                resetToHome()
                return true
            } else {
                val parentFile = File(current).parentFile
                if (parentFile != null && parentFile.absolutePath.startsWith(storageRoot)) {
                    loadFiles(parentFile.absolutePath)
                    return true
                } else {
                    loadFiles(storageRoot)
                    return true
                }
            }
        }
        return false
    }

    private fun isSearchActive(): Boolean {
        return _searchQuery.value.isNotEmpty()
    }

    fun loadCategory(category: FileCategory) {
        _currentPath.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            _currentCategory.value = category
            _searchQuery.value = ""
            repository.getFilesByCategory(category).collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun aiSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = uiState.value
            if (currentState !is ExplorerUiState.Success) return@launch

            _events.send(ExplorerEvent.ShowMessage("AI is searching..."))

            val fileNames = currentState.files.joinToString("\n") { it.name }
            val prompt = "From this list of files, which ones best match the query: '$query'? Return only the filenames that match, separated by newlines. If none match, return 'NONE'.\n\nFiles:\n$fileNames"

            try {
                val response = aiService.chatWithDocument(fileNames, prompt)
                response.collect { result ->
                    if (result != null && result.trim().uppercase() != "NONE") {
                        val matchingNames = result.lines().map { it.trim() }
                        _rawFiles.value = currentState.files.filter { it.name in matchingNames }
                        _events.send(ExplorerEvent.ShowMessage("AI found matches"))
                    }
                }
            } catch (e: Exception) {
                _events.send(ExplorerEvent.ShowMessage("AI Search failed"))
            }
        }
    }

    fun refresh() {
        val category = _currentCategory.value
        val currentPath = _currentPath.value

        when {
            category != null -> loadCategory(category)
            currentPath.isNotEmpty() -> loadFiles(currentPath)
            else -> resetToHome()
        }
    }

    fun toggleFileSelection(path: String) {
        val currentSelection = _selectedFiles.value
        _selectedFiles.value = if (currentSelection.contains(path)) {
            currentSelection - path
        } else {
            currentSelection + path
        }
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    fun selectAll() {
        val currentState = uiState.value
        if (currentState is ExplorerUiState.Success) {
            _selectedFiles.value = currentState.files.map { it.path }.toSet()
        }
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedFiles.value.forEach { path ->
                repository.deleteFile(path)
            }
            clearSelection()
            refresh()
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.renameFile(path, newName)) {
                refresh()
            }
        }
    }

    fun toggleFavorite(file: FileModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(file)
            refresh()
        }
    }

    fun moveToVault(file: FileModel) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.toggleVault(file)) {
                refresh()
            }
        }
    }

    fun startMove(paths: List<String>) {
        _isCopying.value = emptyList()
        _isMoving.value = paths
        _events.trySend(ExplorerEvent.ShowMessage("Selected ${paths.size} items to move"))
    }

    fun startCopy(paths: List<String>) {
        _isMoving.value = emptyList()
        _isCopying.value = paths
        _events.trySend(ExplorerEvent.ShowMessage("Selected ${paths.size} items to copy"))
    }

    fun paste() {
        val targetDir = _currentPath.value
        if (targetDir.isBlank()) {
            _events.trySend(ExplorerEvent.ShowMessage("Cannot paste here"))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val moving = _isMoving.value
            val copying = _isCopying.value

            if (moving.isNotEmpty()) {
                moving.forEach { source ->
                    val sourceFile = File(source)
                    val destFile = File(targetDir, sourceFile.name)
                    repository.moveFile(source, destFile.absolutePath)
                }
                _isMoving.value = emptyList()
                _events.send(ExplorerEvent.ShowMessage("Moved successfully"))
            } else if (copying.isNotEmpty()) {
                copying.forEach { source ->
                    val sourceFile = File(source)
                    val destFile = File(targetDir, sourceFile.name)
                    _events.send(ExplorerEvent.ShowMessage("Copying ${sourceFile.name} to ${destFile.parent}..."))
                }
                _isCopying.value = emptyList()
            }
            refresh()
        }
    }

    fun cancelOperation() {
        _isMoving.value = emptyList()
        _isCopying.value = emptyList()
    }

    fun loadVault() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getVaultFiles().collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun startPickingFolder(archive: FileModel) {
        _pickingFolderForArchive.value = archive
    }

    fun selectAllPaths(paths: List<String>) {
        _selectedFiles.value = paths.toSet()
    }

    fun stopPickingFolder() {
        _pickingFolderForArchive.value = null
    }

    fun extractToCurrentFolder() {
        val archive = _pickingFolderForArchive.value ?: return
        val current = _currentPath.value
        extractArchive(appContext, archive.path, current)
        stopPickingFolder()
    }

    fun extractArchive(context: Context?, path: String, customDestination: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            val destDir = if (customDestination != null) {
                File(customDestination)
            } else {
                File(file.parentFile, file.nameWithoutExtension)
            }

            val targetContext = context ?: appContext

            _events.send(ExplorerEvent.ShowMessage("Extracting ${file.name}..."))

            val success = if (file.extension.lowercase() == "zip") {
                com.sharjeel.fileviewerapp.util.ArchiveUtils.extractZip(file, destDir, targetContext) { msg ->
                    viewModelScope.launch { _events.send(ExplorerEvent.ShowMessage(msg)) }
                }
            } else {
                com.sharjeel.fileviewerapp.util.ArchiveUtils.extractRar(file, destDir, targetContext) { msg ->
                    viewModelScope.launch { _events.send(ExplorerEvent.ShowMessage(msg)) }
                }
            }

            if (success) {
                _events.send(ExplorerEvent.ShowMessage("Extracted successfully to ${destDir.name}"))
                if (destDir.exists() && destDir.isDirectory) {
                    _events.send(ExplorerEvent.NavigateToFolder(destDir.absolutePath, destDir.name))
                } else {
                    refresh()
                }
            }
        }
    }

    fun loadRecent() {
        _currentPath.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            _currentCategory.value = FileCategory.RECENT
            repository.getRecentFiles().collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun loadFavorites() {
        _currentPath.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            _currentCategory.value = FileCategory.FAVORITES
            repository.getFavoriteFiles().collect { files ->
                _rawFiles.value = files
            }
        }
    }
}

@Immutable
data class BreadcrumbItem(
    val name: String,
    val path: String,
    val category: FileCategory? = null
)

@Immutable
sealed interface ExplorerEvent {
    @Immutable
    data class ShowMessage(val message: String) : ExplorerEvent

    @Immutable
    data class NavigateToFolder(val path: String, val title: String) : ExplorerEvent

    object NavigateToHome : ExplorerEvent
}

enum class SortType { NAME, TYPE, SIZE, DATE }
enum class SortOrder { ASCENDING, DESCENDING }
enum class ViewMode { SMALL, MEDIUM, LARGE, LIST }