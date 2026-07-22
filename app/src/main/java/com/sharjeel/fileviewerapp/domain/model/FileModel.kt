package com.sharjeel.fileviewerapp.domain.model

import androidx.compose.runtime.Immutable
import com.sharjeel.fileviewerapp.util.FileUtils
import java.io.File

@Immutable
data class FileModel(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val extension: String = "",
    val itemCount: Int = 0,
    val mimeType: String? = null,
    val formattedSize: String = "",
    val formattedDate: String = "",
    val fileType: FileCategory = FileCategory.OTHER
) {
    companion object {
        fun fromFile(file: File, countItems: Boolean = false): FileModel {
            val isDir = file.isDirectory // Single Disk Read
            val ext = if (isDir) "" else (file.extension.lowercase())
            val size = if (isDir) 0L else file.length()
            val lastModified = file.lastModified()

            // Heavy list() call only if explicitly requested, otherwise 0
            val count = if (isDir && countItems) {
                file.list()?.size ?: 0
            } else 0

            return FileModel(
                name = file.name,
                path = file.absolutePath,
                size = size,
                lastModified = lastModified,
                isDirectory = isDir,
                extension = ext,
                itemCount = count,
                formattedSize = if (isDir) "$count Items" else FileUtils.formatFileSize(size),
                formattedDate = FileUtils.formatDate(lastModified),
                fileType = getCategoryFromExtension(isDir, ext)
            )
        }

        private fun getCategoryFromExtension(isDir: Boolean, ext: String): FileCategory {
            if (isDir) return FileCategory.FOLDER
            return when (ext) {
                "jpg", "jpeg", "png", "webp", "gif", "heic", "bmp" -> FileCategory.IMAGE
                "mp4", "mkv", "avi", "mov", "3gp", "webm", "flv" -> FileCategory.VIDEO
                "mp3", "wav", "flac", "opus", "ogg", "m4a", "aac" -> FileCategory.AUDIO
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "json", "xml", "log" -> FileCategory.DOCUMENT
                "zip", "rar", "7z", "tar", "gz", "bz2" -> FileCategory.ARCHIVE
                "apk", "xapk" -> FileCategory.APK
                else -> FileCategory.OTHER
            }
        } }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileModel) return false

        if (path != other.path) return false
        if (size != other.size) return false
        if (lastModified != other.lastModified) return false
        if (isDirectory != other.isDirectory) return false
        if (itemCount != other.itemCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + isDirectory.hashCode()
        result = 31 * result + itemCount.hashCode()
        return result
    }
}

enum class FileCategory {
    FOLDER, IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, APK, OTHER
}