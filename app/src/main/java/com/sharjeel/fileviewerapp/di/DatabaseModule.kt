package com.sharjeel.fileviewerapp.di

import android.content.Context
import androidx.room.Room
import com.sharjeel.fileviewerapp.data.local.AppDatabase
import com.sharjeel.fileviewerapp.data.local.dao.FileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "file_viewer_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideFileDao(database: AppDatabase): FileDao {
        return database.fileDao()
    }
}
