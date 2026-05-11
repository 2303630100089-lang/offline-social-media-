package com.meshverse.app.di

import com.meshverse.app.security.CryptoManager
import com.meshverse.app.security.KeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideKeyManager(): KeyManager = KeyManager()

    @Provides
    @Singleton
    fun provideCryptoManager(keyManager: KeyManager): CryptoManager = CryptoManager(keyManager)
}
