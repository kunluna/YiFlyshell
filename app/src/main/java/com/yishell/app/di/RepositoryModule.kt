package com.yishell.app.di

import com.yishell.app.data.repository.ConnectionRepositoryImpl
import com.yishell.app.domain.repository.ConnectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        connectionRepositoryImpl: ConnectionRepositoryImpl
    ): ConnectionRepository
}
