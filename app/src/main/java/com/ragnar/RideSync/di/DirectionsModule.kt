package com.ragnar.RideSync.di

import com.ragnar.RideSync.data.repository.MapboxDirectionsRepository
import com.ragnar.RideSync.domain.repository.DirectionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt binding for Phase 10 directions routing. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DirectionsModule {

    @Binds
    @Singleton
    abstract fun bindDirectionsRepository(impl: MapboxDirectionsRepository): DirectionsRepository
}
