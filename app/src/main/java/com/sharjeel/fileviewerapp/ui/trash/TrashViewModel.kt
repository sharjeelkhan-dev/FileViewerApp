package com.sharjeel.fileviewerapp.ui.trash

import android.os.Environment
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: FileRepository
) : ViewModel() {

    val breadcrumbs: StateFlow<List<BreadcrumbItem>> = MutableStateFlow(
        listOf(
            BreadcrumbItem("Home", "", null),
            BreadcrumbItem("Internal Storage", Environment.getExternalStorageDirectory().absolutePath, null),
            BreadcrumbItem("Trash", "", null)
        )
    ).asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.SMALL)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.NAME)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    // Combined Flow returning standard ExplorerUiState
    val uiState: StateFlow<ExplorerUiState> = combine(
        repository.getTrashFiles(),
        _sortType,
        _sortOrder
    ) { rawFiles, type, order ->
        val sortedList = sortFiles(rawFiles, type, order)
        ExplorerUiState.Success(sortedList) as ExplorerUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExplorerUiState.Loading
    )

    fun toggleFileSelection(path: String) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
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

    // Single item restore
    fun restoreFile(fileModel: FileModel) {
        viewModelScope.launch {
            repository.restoreFile(fileModel)
            _selectedFiles.value -= fileModel.path
        }
    }

    // Single item delete permanently
    fun deletePermanently(fileModel: FileModel) {
        viewModelScope.launch {
            repository.permanentlyDeleteFile(fileModel)
            _selectedFiles.value -= fileModel.path
        }
    }

    // Bulk restore selected files
    fun restoreSelected() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is ExplorerUiState.Success) {
                val toRestore = currentState.files.filter { _selectedFiles.value.contains(it.path) }
                toRestore.forEach { file ->
                    repository.restoreFile(file)
                }
                clearSelection()
            }
        }
    }

    // Bulk delete selected files permanently
    fun deleteSelectedPermanently() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is ExplorerUiState.Success) {
                val toDelete = currentState.files.filter { _selectedFiles.value.contains(it.path) }
                toDelete.forEach { file ->
                    repository.permanentlyDeleteFile(file)
                }
                clearSelection()
            }
        }
    }

    // Empty entire trash
    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            clearSelection()
        }
    }

    fun updateViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun updateSort(type: SortType, order: SortOrder) {
        _sortType.value = type
        _sortOrder.value = order
    }

    private fun sortFiles(
        files: List<FileModel>,
        sortType: SortType,
        sortOrder: SortOrder
    ): List<FileModel> {
        val sorted = when (sortType) {
            SortType.NAME -> files.sortedBy { it.name.lowercase() }
            SortType.DATE -> files.sortedBy { it.lastModified }
            SortType.SIZE -> files.sortedBy { it.size }
            SortType.TYPE -> files.sortedBy { it.extension.lowercase() }
        }
        return if (sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }
}