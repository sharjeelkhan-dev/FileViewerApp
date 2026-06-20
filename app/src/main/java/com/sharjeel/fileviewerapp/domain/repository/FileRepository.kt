package com.sharjeel.fileviewerapp.domain.repository

import com.sharjeel.fileviewerapp.domain.model.FileModel
import kotlinx.coroutines.flow.Flow
import java.io.File

interface FileRepository {
    fun getFiles(directory: File): Flow<List<FileModel>>
    fun getFilesByCategory(category: FileCategory): Flow<List<FileModel>>
    suspend fun deleteFile(path: String): Boolean
    suspend fun renameFile(path: String, newName: String): Boolean
    suspend fun moveFile(sourcePath: String, targetPath: String): Boolean

    // Persistence
    fun getRecentFiles(): Flow<List<FileModel>>
    suspend fun addToRecent(file: FileModel)
    fun getFavoriteFiles(): Flow<List<FileModel>>
    suspend fun toggleFavorite(file: FileModel)
    suspend fun isFavorite(path: String): Boolean
    fun getVaultFiles(): Flow<List<FileModel>>
    suspend fun toggleVault(file: FileModel): Boolean

    // Trash
    fun getTrashFiles(): Flow<List<FileModel>>
    suspend fun deleteFileToTrash(file: FileModel): Boolean
    suspend fun restoreFile(file: FileModel): Boolean
    suspend fun permanentlyDeleteFile(file: FileModel): Boolean
    suspend fun emptyTrash(): Boolean
}

enum class FileCategory {
    DOCUMENTS, IMAGES, AUDIO, VIDEOS, ARCHIVES, DOWNLOADS, RECENT, FAVORITES
}
