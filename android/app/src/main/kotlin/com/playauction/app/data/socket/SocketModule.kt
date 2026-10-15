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

    /**
     * Provides a shared [Gson] instance. If your app already provides a
     * [Gson] elsewhere (e.g. a NetworkModule), remove this method to avoid
     * duplicate binding and let Hilt pick up the existing one.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    /**
     * Explicitly documents that [SocketManager] is available in the graph.
     * Because [SocketManager] is constructor-injected with @Singleton, Hilt
     * resolves it without an @Provides method, but this companion object keeps
     * all socket-related bindings in one place for discoverability.
     *
     * Downstream consumers simply `@Inject constructor(socketManager: SocketManager)`.
     */
    // No @Provides needed for SocketManager — Hilt auto-binds @Inject @Singleton classes.
}
