package com.sharjeel.fileviewerapp.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.domain.repository.FileRepository
import com.sharjeel.fileviewerapp.ui.explorer.BreadcrumbItem
import com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState
import com.sharjeel.fileviewerapp.ui.explorer.SortOrder
import com.sharjeel.fileviewerapp.ui.explorer.SortType
import com.sharjeel.fileviewerapp.ui.explorer.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: FileRepository
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _uiState = MutableStateFlow<ExplorerUiState>(ExplorerUiState.Loading)
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.SMALL)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.NAME)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    val breadcrumbs: StateFlow<List<BreadcrumbItem>> = MutableStateFlow(
        listOf(
            BreadcrumbItem("Home", "", null),
            BreadcrumbItem("Internal Storage", android.os.Environment.getExternalStorageDirectory().absolutePath, null),
            BreadcrumbItem("Vault", "", null)
        )
    ).asStateFlow()

    fun updateViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun toggleFileSelection(path: String) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(path)) current.remove(path)
        else current.add(path)
        _selectedFiles.value = current
    }

    fun selectAll() {
        val currentState = _uiState.value
        if (currentState is ExplorerUiState.Success) {
            _selectedFiles.value = currentState.files.map { it.path }.toSet()
        }
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    fun updateSort(type: SortType, order: SortOrder) {
        _sortType.value = type
        _sortOrder.value = order
        loadVaultFiles()
    }

    fun unlock() {
        _isUnlocked.value = true
        loadVaultFiles()
    }

    fun lock() {
        _isUnlocked.value = false
    }

    private fun loadVaultFiles() {
        viewModelScope.launch {
            _uiState.value = ExplorerUiState.Loading
            repository.getVaultFiles().collect { files ->
                _uiState.value = ExplorerUiState.Success(files)
            }
        }
    }

    fun removeFromVault(file: FileModel) {
        viewModelScope.launch {
            if (repository.toggleVault(file)) {
                loadVaultFiles()
            }
        }
    }

    fun removeSelectedFromVault() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ExplorerUiState.Success) {
                val toRemove = currentState.files.filter { _selectedFiles.value.contains(it.path) }
                toRemove.forEach { file ->
                    repository.toggleVault(file)
                }
                clearSelection()
                loadVaultFiles()
            }
        }
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            _selectedFiles.value.forEach { path ->
                repository.deleteFile(path)
            }
            clearSelection()
            loadVaultFiles()
        }
    }
    
    fun deleteFile(path: String) {
        viewModelScope.launch {
            if (repository.deleteFile(path)) {
                loadVaultFiles()
            }
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
            if (repository.renameFile(path, newName)) {
                loadVaultFiles()
            }
        }
    }
}
