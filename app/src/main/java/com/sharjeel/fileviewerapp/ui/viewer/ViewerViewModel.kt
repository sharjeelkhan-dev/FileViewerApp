package com.sharjeel.fileviewerapp.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: FileRepository
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // Universal playlist for all file types in the current view
    private val _filePlaylist = MutableStateFlow<List<FileModel>>(emptyList())
    val filePlaylist: StateFlow<List<FileModel>> = _filePlaylist.asStateFlow()

    private val _currentFileIndex = MutableStateFlow(-1)
    val currentFileIndex: StateFlow<Int> = _currentFileIndex.asStateFlow()

    fun setPlaylist(files: List<FileModel>, initialPath: String) {
        _filePlaylist.value = files
        _currentFileIndex.value = files.indexOfFirst { it.path == initialPath }
    }

    /**
     * Fallback for external intents: Loads all viewable files in the same directory.
     */
    fun loadFolderPlaylist(currentFilePath: String) {
        // Don't overwrite if playlist is already set (e.g. from Categories/Explorer)
        if (_filePlaylist.value.isNotEmpty()) return
        
        viewModelScope.launch {
            val file = java.io.File(currentFilePath)
            val parentDir = file.parentFile
            if (parentDir != null && parentDir.exists()) {
                val files = parentDir.listFiles()
                    ?.filter { it.isFile && !it.name.startsWith(".") }
                    ?.map { FileModel.fromFile(it) }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
                
                _filePlaylist.value = files
                _currentFileIndex.value = files.indexOfFirst { it.path == currentFilePath }
            }
        }
    }

    fun checkIfFavorite(path: String) {
        viewModelScope.launch {
            _isFavorite.value = repository.isFavorite(path)
        }
    }

    fun toggleFavorite(file: FileModel) {
        viewModelScope.launch {
            repository.toggleFavorite(file)
            _isFavorite.value = !_isFavorite.value
        }
    }

    fun addToRecent(file: FileModel) {
        viewModelScope.launch {
            repository.addToRecent(file)
        }
    }

    fun updateFileIndex(index: Int) {
        if (index in _filePlaylist.value.indices) {
            _currentFileIndex.value = index
        }
    }
}
