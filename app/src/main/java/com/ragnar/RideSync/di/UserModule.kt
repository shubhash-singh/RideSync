package com.ragnar.RideSync.di

import com.ragnar.RideSync.data.repository.FirestoreUserRepository
import com.ragnar.RideSync.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt bindings for user profile data (Phase 4). */
@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirestoreUserRepository): UserRepository
}

