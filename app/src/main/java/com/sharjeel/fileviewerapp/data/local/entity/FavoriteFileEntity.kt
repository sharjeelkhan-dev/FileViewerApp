package com.sharjeel.fileviewerapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_files")
data class FavoriteFileEntity(
    @PrimaryKey val path: String,
    val name: String,
    val timestamp: Long,
    val type: String
)
