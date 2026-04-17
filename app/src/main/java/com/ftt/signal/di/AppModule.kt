package com.ftt.signal.di

import android.content.Context
import com.ftt.signal.NotificationHelper
import com.ftt.signal.prefs.AppPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for app-wide singletons that don't belong to
 * the network or database modules.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppPrefs(@ApplicationContext ctx: Context): AppPrefs = AppPrefs(ctx)

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext ctx: Context): NotificationHelper =
        NotificationHelper(ctx)
}
