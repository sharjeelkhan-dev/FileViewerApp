package com.sharjeel.fileviewerapp.ui.explorer

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.domain.repository.FileCategory
import com.sharjeel.fileviewerapp.domain.repository.FileRepository
import com.sharjeel.fileviewerapp.util.AIService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val repository: FileRepository,
    private val aiService: AIService
) : ViewModel() {

    private val _events = Channel<ExplorerEvent>()
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

    val uiState: StateFlow<ExplorerUiState> = combine(
        _rawFiles, 
        _searchQuery,
        _sortType,
        _sortOrder
    ) { files, query, sortType, sortOrder ->
        val filtered = if (query.isBlank()) {
            files
        } else {
            files.filter { it.name.contains(query, ignoreCase = true) }
        }

        val sorted = when (sortType) {
            SortType.NAME -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.name.lowercase() } else filtered.sortedByDescending { it.name.lowercase() }
            SortType.TYPE -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.extension.lowercase() } else filtered.sortedByDescending { it.extension.lowercase() }
            SortType.SIZE -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.size } else filtered.sortedByDescending { it.size }
            SortType.DATE -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.lastModified } else filtered.sortedByDescending { it.lastModified }
        }

        ExplorerUiState.Success(sorted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExplorerUiState.Loading
    )

    private val _currentPath = MutableStateFlow(Environment.getExternalStorageDirectory().absolutePath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _currentCategory = MutableStateFlow<FileCategory?>(null)

    fun setSort(type: SortType, order: SortOrder) {
        _sortType.value = type
        _sortOrder.value = order
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _currentPath.value = path
            _currentCategory.value = null
            repository.getFiles(File(path)).collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun loadCategory(category: FileCategory) {
        _currentPath.value = ""
        viewModelScope.launch {
            _currentCategory.value = category
            repository.getFilesByCategory(category).collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        // If it's a complex natural language query, we could trigger AI filtering here
    }

    fun aiSearch(query: String) {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState !is ExplorerUiState.Success) return@launch
            
            _events.send(ExplorerEvent.ShowMessage("AI is searching..."))
            
            val fileNames = currentState.files.joinToString("\n") { it.name }
            val prompt = "From this list of files, which ones best match the query: '$query'? Return only the filenames that match, separated by newlines. If none match, return 'NONE'.\n\nFiles:\n$fileNames"
            
            try {
                // Using provideGenerativeModel from di concept
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
            else -> loadFiles(currentPath)
        }
    }

    fun toggleFileSelection(path: String) {
        val currentSelection = _selectedFiles.value.toMutableSet()
        if (currentSelection.contains(path)) {
            currentSelection.remove(path)
        } else {
            currentSelection.add(path)
        }
        _selectedFiles.value = currentSelection
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
        viewModelScope.launch {
            _selectedFiles.value.forEach { path ->
                repository.deleteFile(path)
            }
            clearSelection()
            refresh()
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
            if (repository.renameFile(path, newName)) {
                refresh()
            }
        }
    }

    fun toggleFavorite(file: FileModel) {
        viewModelScope.launch {
            repository.toggleFavorite(file)
            refresh()
        }
    }

    fun moveToVault(file: FileModel) {
        viewModelScope.launch {
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

        viewModelScope.launch {
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

                    _events.send(ExplorerEvent.ShowMessage("Copy feature implementation pending in repository"))
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
        viewModelScope.launch {
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
        extractArchive(null, archive.path, current)
        stopPickingFolder()
    }

    private var appContext: android.content.Context? = null
    fun setContext(context: android.content.Context) {
        this.appContext = context.applicationContext
    }

    fun extractArchive(context: android.content.Context?, path: String, customDestination: String? = null) {
        viewModelScope.launch {
            val file = File(path)
            val destDir = if (customDestination != null) {
                File(customDestination)
            } else {
                File(file.parentFile, file.nameWithoutExtension)
            }
            
            val targetContext = context ?: appContext
            if (targetContext == null) {
                _events.send(ExplorerEvent.ShowMessage("System error: Missing context"))
                return@launch
            }
            
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
                // Request navigation to the new folder
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
        viewModelScope.launch {
            _currentCategory.value = FileCategory.RECENT
            repository.getRecentFiles().collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun loadFavorites() {
        _currentPath.value = ""
        viewModelScope.launch {
            _currentCategory.value = FileCategory.FAVORITES
            repository.getFavoriteFiles().collect { files ->
                _rawFiles.value = files
            }
        }
    }
}

sealed interface ExplorerEvent {
    data class ShowMessage(val message: String) : ExplorerEvent
    data class NavigateToFolder(val path: String, val title: String) : ExplorerEvent
}

sealed interface ExplorerUiState {
    data object Loading : ExplorerUiState
    data class Success(val files: List<FileModel>) : ExplorerUiState
    data class Error(val message: String) : ExplorerUiState
}

enum class SortType { NAME, TYPE, SIZE, DATE }
enum class SortOrder { ASCENDING, DESCENDING }
enum class ViewMode { SMALL, MEDIUM, LARGE, LIST }
