package com.sharjeel.fileviewerapp.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.fileviewerapp.domain.repository.FileRepository
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
class TrashViewModel @Inject constructor(
    private val repository: FileRepository
) : ViewModel() {

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

    init {
        loadTrashFiles()
    }

    fun loadTrashFiles() {
        viewModelScope.launch {
            _uiState.value = ExplorerUiState.Loading
            repository.getTrashFiles().collect { files ->
                _uiState.value = ExplorerUiState.Success(files)
            }
        }
    }

    fun toggleFileSelection(path: String) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(path)) current.remove(path)
        else current.add(path)
        _selectedFiles.value = current
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

    fun restoreSelected() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is ExplorerUiState.Success) {
                val toRestore = currentState.files.filter { _selectedFiles.value.contains(it.path) }
                toRestore.forEach {
                    repository.restoreFile(it)
                }
                clearSelection()
                loadTrashFiles()
            }
        }
    }

    fun restoreAll() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is ExplorerUiState.Success) {
                currentState.files.forEach {
                    repository.restoreFile(it)
                }
                clearSelection()
                loadTrashFiles()
            }
        }
    }

    fun deleteSelectedPermanently() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is ExplorerUiState.Success) {
                val toDelete = currentState.files.filter { _selectedFiles.value.contains(it.path) }
                toDelete.forEach {
                    repository.permanentlyDeleteFile(it)
                }
                clearSelection()
                loadTrashFiles()
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            clearSelection()
            loadTrashFiles()
        }
    }

    fun updateViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun updateSort(type: SortType, order: SortOrder) {
        _sortType.value = type
        _sortOrder.value = order
    }
}
