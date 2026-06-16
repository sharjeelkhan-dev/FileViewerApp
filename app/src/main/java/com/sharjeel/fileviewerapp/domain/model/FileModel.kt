package com.sharjeel.fileviewerapp.domain.model

import java.io.File

data class FileModel(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val extension: String = "",
    val itemCount: Int = 0,
    val mimeType: String? = null
) {
    companion object {
        fun fromFile(file: File, countItems: Boolean = false): FileModel {
            return FileModel(
                name = file.name,
                path = file.absolutePath,
                size = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified(),
                isDirectory = file.isDirectory,
                extension = file.extension,
                itemCount = if (file.isDirectory && countItems) file.list()?.size ?: 0 else 0
            )
        }
    }
}
