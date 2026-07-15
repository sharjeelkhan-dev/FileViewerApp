package com.sharjeel.fileviewerapp.ui.favorites

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
import android.os.Environment

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExplorerUiState>(ExplorerUiState.Loading)
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    // 🎯 ADDED: Breadcrumbs for Favorites
    val breadcrumbs: StateFlow<List<BreadcrumbItem>> = MutableStateFlow(
        listOf(
            BreadcrumbItem("Internal Storage", Environment.getExternalStorageDirectory().absolutePath, null),
            BreadcrumbItem("Favorites", "", null)
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

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = ExplorerUiState.Loading
            repository.getFavoriteFiles().collect { files ->
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

    fun toggleFavorite(file: FileModel) {
        viewModelScope.launch {
            repository.toggleFavorite(file)
            loadFavorites()
        }
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            _selectedFiles.value.forEach { path ->
                repository.deleteFile(path)
            }
            clearSelection()
            loadFavorites()
        }
    }

    fun updateViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun updateSort(type: SortType, order: SortOrder) {
        _sortType.value = type
        _sortOrder.value = order
        loadFavorites()
    }
}
