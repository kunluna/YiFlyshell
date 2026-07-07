package com.yishell.app.di

import android.content.Context
import androidx.room.Room
import com.yishell.app.data.local.AppDatabase
import com.yishell.app.data.local.ConnectionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "yishell.db"
        )
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .setQueryExecutor(Executors.newFixedThreadPool(4))
            .build()
    }

    @Provides
    @Singleton
    fun provideConnectionDao(database: AppDatabase): ConnectionDao {
        return database.connectionDao()
    }
}
