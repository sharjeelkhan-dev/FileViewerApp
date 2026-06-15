package com.sharjeel.fileviewerapp.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.domain.repository.FileRepository
import com.sharjeel.fileviewerapp.ui.explorer.ExplorerUiState
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
