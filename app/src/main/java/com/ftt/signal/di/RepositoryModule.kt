package com.ftt.signal.di

import com.ftt.signal.data.repository.JournalRepositoryImpl
import com.ftt.signal.data.repository.SignalRepositoryImpl
import com.ftt.signal.domain.repository.IJournalRepository
import com.ftt.signal.domain.repository.ISignalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds domain interfaces to their data-layer
 * implementations.  Using @Binds (not @Provides) is more efficient
 * as it avoids creating a wrapper method at compile time.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSignalRepository(impl: SignalRepositoryImpl): ISignalRepository

    @Binds
    @Singleton
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): IJournalRepository
}
