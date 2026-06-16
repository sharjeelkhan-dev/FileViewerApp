package com.sharjeel.fileviewerapp.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.sharjeel.fileviewerapp.data.local.dao.FileDao
import com.sharjeel.fileviewerapp.data.local.entity.FavoriteFileEntity
import com.sharjeel.fileviewerapp.data.local.entity.RecentFileEntity
import com.sharjeel.fileviewerapp.domain.model.FileModel
import com.sharjeel.fileviewerapp.domain.repository.FileCategory
import com.sharjeel.fileviewerapp.domain.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDao: FileDao
) : FileRepository {

    override fun getFiles(directory: File): Flow<List<FileModel>> = flow {
        // Robust directory check
        val targetDir = if (directory.absolutePath.isEmpty()) {
            Environment.getExternalStorageDirectory()
        } else directory

        val files = if (targetDir.exists() && targetDir.isDirectory) {
            targetDir.listFiles()?.map { FileModel.fromFile(it, countItems = true) } ?: emptyList()
        } else {
            // Fallback to internal storage root if path doesn't exist
            Environment.getExternalStorageDirectory().listFiles()?.map { FileModel.fromFile(it, countItems = true) } ?: emptyList()
        }
        
        emit(files.sortedWith(compareByDescending<FileModel> { it.isDirectory }.thenBy { it.name }))
    }.flowOn(Dispatchers.IO)

    override fun getFilesByCategory(category: FileCategory): Flow<List<FileModel>> = flow {
        val files = when (category) {
            FileCategory.DOWNLOADS -> {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.listFiles()?.map { FileModel.fromFile(it, countItems = false) } ?: emptyList()
            }
            FileCategory.IMAGES -> queryMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            FileCategory.VIDEOS -> queryMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            FileCategory.AUDIO -> queryMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            FileCategory.DOCUMENTS -> queryMediaStore(MediaStore.Files.getContentUri("external"), isDocument = true)
            FileCategory.ARCHIVES -> queryAllFilesForArchives()
            FileCategory.RECENT -> getRecentFiles().first()
            FileCategory.FAVORITES -> getFavoriteFiles().first()
            else -> emptyList()
        }
        emit(files.sortedByDescending { it.lastModified })
    }.flowOn(Dispatchers.IO)

    private fun queryMediaStore(uri: android.net.Uri, isDocument: Boolean = false): List<FileModel> {
        val fileList = mutableListOf<FileModel>()
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        
        val selection = if (isDocument) {
            "${MediaStore.Files.FileColumns.MIME_TYPE} IN ('application/pdf', 'application/msword', 'text/plain', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')"
        } else null

        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataIndex)
                if (path != null) {
                    val file = File(path)
                    fileList.add(
                        FileModel(
                            name = cursor.getString(nameIndex) ?: file.name,
                            path = path,
                            size = cursor.getLong(sizeIndex),
                            lastModified = cursor.getLong(dateIndex) * 1000,
                            isDirectory = false,
                            extension = file.extension,
                            mimeType = cursor.getString(mimeIndex)
                        )
                    )
                }
            }
        }
        return fileList
    }

    private fun queryAllFilesForArchives(): List<FileModel> {
        val extensions = listOf("zip", "rar", "7z", "tar")
        return walkExternalStorage(extensions)
    }

    private fun walkExternalStorage(extensions: List<String>): List<FileModel> {
        val result = mutableListOf<FileModel>()
        val root = Environment.getExternalStorageDirectory()
        
        fun walk(dir: File) {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    if (!file.name.startsWith(".")) {
                        walk(file)
                    }
                } else {
                    if (extensions.contains(file.extension.lowercase())) {
                        result.add(FileModel.fromFile(file, countItems = false))
                    }
                }
            }
        }
        
        walk(root)
        return result
    }

    override suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        } else false
    }

    override suspend fun renameFile(path: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists()) {
            val newFile = File(file.parent, newName)
            if (file.renameTo(newFile)) {
                true
            } else {
                try {
                    file.copyTo(newFile, overwrite = true)
                    file.delete()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } else false
    }

    override suspend fun moveFile(sourcePath: String, targetPath: String): Boolean = withContext(Dispatchers.IO) {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return@withContext false
        
        val targetFile = File(targetPath, sourceFile.name)
        if (sourceFile.renameTo(targetFile)) {
            true
        } else {
            try {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun getRecentFiles(): Flow<List<FileModel>> = fileDao.getRecentFiles().map { entities ->
        entities.mapNotNull { entity ->
            val file = File(entity.path)
            if (file.exists()) {
                FileModel(
                    name = entity.name,
                    path = entity.path,
                    size = file.length(),
                    lastModified = entity.timestamp,
                    isDirectory = false,
                    extension = file.extension,
                    mimeType = entity.type
                )
            } else null
        }
    }

    override suspend fun addToRecent(file: FileModel) {
        fileDao.insertRecentFile(
            RecentFileEntity(
                path = file.path,
                name = file.name,
                timestamp = System.currentTimeMillis(),
                type = file.mimeType ?: ""
            )
        )
    }

    override fun getFavoriteFiles(): Flow<List<FileModel>> = fileDao.getFavoriteFiles().map { entities ->
        entities.mapNotNull { entity ->
            val file = File(entity.path)
            if (file.exists()) {
                FileModel(
                    name = entity.name,
                    path = entity.path,
                    size = file.length(),
                    lastModified = entity.timestamp,
                    isDirectory = false,
                    extension = file.extension,
                    mimeType = entity.type
                )
            } else null
        }
    }

    override suspend fun toggleFavorite(file: FileModel) {
        val isFav = isFavorite(file.path)
        if (isFav) {
            fileDao.deleteFavoriteFile(FavoriteFileEntity(file.path, file.name, file.lastModified, file.mimeType ?: ""))
        } else {
            fileDao.insertFavoriteFile(FavoriteFileEntity(file.path, file.name, System.currentTimeMillis(), file.mimeType ?: ""))
        }
    }

    override suspend fun isFavorite(path: String): Boolean = withContext(Dispatchers.IO) {
        fileDao.isFavorite(path)
    }

    override fun getVaultFiles(): Flow<List<FileModel>> = flow {
        val vaultDir = File(context.filesDir, ".vault")
        if (!vaultDir.exists()) vaultDir.mkdirs()
        val files = vaultDir.listFiles()?.map { FileModel.fromFile(it, countItems = false) } ?: emptyList()
        emit(files.sortedByDescending { it.lastModified })
    }.flowOn(Dispatchers.IO)

    override suspend fun toggleVault(file: FileModel): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, ".vault")
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val sourceFile = File(file.path)
            if (!sourceFile.exists()) return@withContext false

            val isAlreadyInVault = sourceFile.parentFile?.absolutePath == vaultDir.absolutePath

            val targetFile = if (isAlreadyInVault) {
                val restoreDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!restoreDir.exists()) restoreDir.mkdirs()
                File(restoreDir, sourceFile.name)
            } else {
                File(vaultDir, sourceFile.name)
            }

            // Using copyTo + delete instead of renameTo for cross-volume reliability
            if (sourceFile.renameTo(targetFile)) {
                true
            } else {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
