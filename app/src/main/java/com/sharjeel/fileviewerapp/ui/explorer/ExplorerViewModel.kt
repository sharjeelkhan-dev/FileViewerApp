package com.sharjeel.fileviewerapp.ui.explorer

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.domain.repository.FileCategory
import com.sharjeel.fileviewerapp.domain.repository.FileRepository
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
    private val repository: FileRepository
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
        viewModelScope.launch {
            _currentCategory.value = category
            repository.getFilesByCategory(category).collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        val category = _currentCategory.value
        val currentPath = _currentPath.value
        
        // If we are in a special view like Recent, Favorites, or Vault, reload that.
        // We can detect this by checking if the path ends with specific markers or checking _rawFiles source
        // but for now, the most reliable way is checking what the current title suggests or just re-running last command.
        
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

    fun loadVault() {
        viewModelScope.launch {
            repository.getVaultFiles().collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun extractArchive(path: String) {
        viewModelScope.launch {
            val file = File(path)
            val destination = File(file.parentFile, file.nameWithoutExtension)
            
            _events.send(ExplorerEvent.ShowMessage("Extracting..."))
            
            val success = if (file.extension.lowercase() == "zip") {
                com.sharjeel.fileviewerapp.util.ArchiveUtils.extractZip(file, destination)
            } else {
                com.sharjeel.fileviewerapp.util.ArchiveUtils.extractRar(file, destination)
            }
            
            if (success) {
                _events.send(ExplorerEvent.ShowMessage("Extracted successfully to ${destination.name}"))
                refresh()
            } else {
                _events.send(ExplorerEvent.ShowMessage("Extraction failed"))
            }
        }
    }

    fun loadRecent() {
        viewModelScope.launch {
            _currentCategory.value = FileCategory.RECENT
            repository.getRecentFiles().collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun loadFavorites() {
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
}

sealed interface ExplorerUiState {
    data object Loading : ExplorerUiState
    data class Success(val files: List<FileModel>) : ExplorerUiState
    data class Error(val message: String) : ExplorerUiState
}

enum class SortType { NAME, TYPE, SIZE, DATE }
enum class SortOrder { ASCENDING, DESCENDING }
enum class ViewMode { SMALL, MEDIUM, LARGE }
