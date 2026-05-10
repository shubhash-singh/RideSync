package com.ragnar.RideSync.di

import com.ragnar.RideSync.data.repository.FirestoreTeamRepository
import com.ragnar.RideSync.domain.repository.TeamRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [FirestoreTeamRepository] to [TeamRepository].
 *
 * Phase 7: Team Creation & Joining System
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TeamModule {

    @Binds @Singleton abstract fun bindTeamRepository(impl: FirestoreTeamRepository): TeamRepository
}
