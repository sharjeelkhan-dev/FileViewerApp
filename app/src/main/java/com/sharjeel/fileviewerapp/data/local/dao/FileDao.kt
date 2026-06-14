package com.sharjeel.fileviewerapp.data.local.dao

import androidx.room.*
import com.sharjeel.fileviewerapp.data.local.entity.FavoriteFileEntity
import com.sharjeel.fileviewerapp.data.local.entity.RecentFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM recent_files ORDER BY timestamp DESC")
    fun getRecentFiles(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(file: RecentFileEntity)

    @Query("SELECT * FROM favorite_files ORDER BY timestamp DESC")
    fun getFavoriteFiles(): Flow<List<FavoriteFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteFile(file: FavoriteFileEntity)

    @Delete
    suspend fun deleteFavoriteFile(file: FavoriteFileEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_files WHERE path = :path)")
    suspend fun isFavorite(path: String): Boolean
}
