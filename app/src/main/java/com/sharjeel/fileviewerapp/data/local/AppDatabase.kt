package com.sharjeel.fileviewerapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sharjeel.fileviewerapp.data.local.dao.FileDao
import com.sharjeel.fileviewerapp.data.local.entity.RecentFileEntity
import com.sharjeel.fileviewerapp.data.local.entity.FavoriteFileEntity

@Database(entities = [RecentFileEntity::class, FavoriteFileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
}
