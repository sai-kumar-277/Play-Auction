package com.playauction.app.data.socket

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [SocketManager] as a process-wide singleton.
 *
 * [SocketManager] is itself annotated with @Singleton and uses constructor
 * injection, so Hilt resolves it automatically. This module provides the
 * [Gson] instance required by [SocketManager] for JSON deserialization.
 *
 * Install in [SingletonComponent] so the same socket connection survives
 * across all Activities, Fragments, and ViewModels.
 */
@Module
@InstallIn(SingletonComponent::class)
object SocketModule {

    // Gson is provided as a Singleton by NetworkModule.
    // SocketManager is constructor-injected with @Singleton, so Hilt binds it automatically.
}
