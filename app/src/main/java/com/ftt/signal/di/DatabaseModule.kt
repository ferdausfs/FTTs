package com.ftt.signal.di

import android.content.Context
import androidx.room.Room
import com.ftt.signal.db.AppDatabase
import com.ftt.signal.db.JournalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides Room database and DAO instances.
 * Installed in [SingletonComponent] so there is exactly one DB
 * instance for the lifetime of the application process.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "ftt_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideJournalDao(db: AppDatabase): JournalDao = db.journalDao()
}
