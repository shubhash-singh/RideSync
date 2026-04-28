package com.ragnar.RideSync.di

import com.ragnar.RideSync.data.repository.FusedLocationRepository
import com.ragnar.RideSync.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [FusedLocationRepository] to the [LocationRepository] interface.
 *
 * Phase 6: Real-Time Location Tracking
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: FusedLocationRepository): LocationRepository
}
