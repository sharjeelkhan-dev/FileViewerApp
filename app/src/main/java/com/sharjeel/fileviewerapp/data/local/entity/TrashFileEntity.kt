package com.sharjeel.fileviewerapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_files")
data class TrashFileEntity(
    @PrimaryKey val originalPath: String,
    val name: String,
    val size: Long,
    val deleteTimestamp: Long,
    val isDirectory: Boolean,
    val mimeType: String? = null
)
