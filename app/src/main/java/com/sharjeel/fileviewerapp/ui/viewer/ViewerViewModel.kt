package com.sharjeel.fileviewerapp.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: FileRepository
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _filePlaylist = MutableStateFlow<List<FileModel>>(emptyList())
    val filePlaylist: StateFlow<List<FileModel>> = _filePlaylist.asStateFlow()

    private val _currentFileIndex = MutableStateFlow(-1)
    val currentFileIndex: StateFlow<Int> = _currentFileIndex.asStateFlow()

    fun setPlaylist(files: List<FileModel>, initialPath: String) {
        _filePlaylist.value = files
        _currentFileIndex.value = files.indexOfFirst { it.path == initialPath }
    }
    fun loadFolderPlaylist(currentFilePath: String) {
        if (_filePlaylist.value.isNotEmpty()) return

        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                try {
                    val file = java.io.File(currentFilePath)
                    val parentDir = file.parentFile
                    if (parentDir != null && parentDir.exists()) {
                        parentDir.listFiles()
                            ?.filter { it.isFile && !it.name.startsWith(".") }
                            ?.map { FileModel.fromFile(it) }
                            ?.sortedBy { it.name.lowercase() }
                            ?: emptyList()
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            _filePlaylist.value = files
            _currentFileIndex.value = files.indexOfFirst { it.path == currentFilePath }
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
            _isFavorite.update { !it }
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