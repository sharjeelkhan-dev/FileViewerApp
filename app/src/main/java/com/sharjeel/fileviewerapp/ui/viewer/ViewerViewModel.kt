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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: FileRepository
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _videoPlaylist = MutableStateFlow<List<FileModel>>(emptyList())
    val videoPlaylist: StateFlow<List<FileModel>> = _videoPlaylist.asStateFlow()

    private val _currentVideoIndex = MutableStateFlow(-1)
    val currentVideoIndex: StateFlow<Int> = _currentVideoIndex.asStateFlow()

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

    fun loadVideoPlaylist(currentFilePath: String) {
        viewModelScope.launch {
            val file = File(currentFilePath)
            val parentDir = file.parentFile
            if (parentDir != null && parentDir.exists()) {
                val videoExtensions = setOf("mp4", "mkv", "avi", "3gp", "webm")
                val videos = parentDir.listFiles()
                    ?.filter { it.isFile && it.extension.lowercase() in videoExtensions }
                    ?.map { FileModel.fromFile(it) }
                    ?.sortedBy { it.name }
                    ?: emptyList()
                
                _videoPlaylist.value = videos
                _currentVideoIndex.value = videos.indexOfFirst { it.path == currentFilePath }
            }
        }
    }

    fun playNextVideo() {
        val nextIndex = _currentVideoIndex.value + 1
        if (nextIndex < _videoPlaylist.value.size) {
            _currentVideoIndex.value = nextIndex
        }
    }

    fun playPreviousVideo() {
        val prevIndex = _currentVideoIndex.value - 1
        if (prevIndex >= 0) {
            _currentVideoIndex.value = prevIndex
        }
    }
}
