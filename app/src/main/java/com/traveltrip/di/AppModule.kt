package com.traveltrip.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for providing application-level dependencies. Bindings for repositories and other
 * singletons will be added here as the project grows through subsequent phases.
 */
@Module @InstallIn(SingletonComponent::class) abstract class AppModule
