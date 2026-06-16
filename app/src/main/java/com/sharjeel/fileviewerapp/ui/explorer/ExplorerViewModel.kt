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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val repository: FileRepository
) : ViewModel() {

    private val _rawFiles = MutableStateFlow<List<FileModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<ExplorerUiState> = combine(_rawFiles, _searchQuery) { files, query ->
        if (query.isBlank()) {
            ExplorerUiState.Success(files)
        } else {
            val filtered = files.filter { it.name.contains(query, ignoreCase = true) }
            ExplorerUiState.Success(filtered)
        }
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
        if (category != null) {
            loadCategory(category)
        } else {
            loadFiles(_currentPath.value)
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

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            _selectedFiles.value.forEach { path ->
                repository.deleteFile(path)
            }
            clearSelection()
            loadFiles(_currentPath.value)
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
            val success = if (file.extension.lowercase() == "zip") {
                com.sharjeel.fileviewerapp.util.ArchiveUtils.extractZip(file, destination)
            } else {
                com.sharjeel.fileviewerapp.util.ArchiveUtils.extractRar(file, destination)
            }
            if (success) {
                loadFiles(_currentPath.value)
            }
        }
    }

    fun loadRecent() {
        viewModelScope.launch {
            repository.getRecentFiles().collect { files ->
                _rawFiles.value = files
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            repository.getFavoriteFiles().collect { files ->
                _rawFiles.value = files
            }
        }
    }
}

// Extension is no longer needed
// private fun <T> kotlinx.coroutines.flow.Flow<T>.asStateFlow(...)

sealed interface ExplorerUiState {
    data object Loading : ExplorerUiState
    data class Success(val files: List<FileModel>) : ExplorerUiState
    data class Error(val message: String) : ExplorerUiState
}
