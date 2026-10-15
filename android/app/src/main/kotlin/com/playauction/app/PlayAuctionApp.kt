package com.playauction.app

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point for PlayAuction.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation and
 * attach the application-level dependency graph. Firebase and Timber are
 * initialized here so that every component that launches after this point
 * can safely use them.
 */
@HiltAndroidApp
class PlayAuctionApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── Firebase ──────────────────────────────────────────────────────────
        // FirebaseApp.initializeApp is idempotent but calling it explicitly
        // ensures initialisation happens before any coroutine or background
        // work fires up.
        FirebaseApp.initializeApp(this)

        // ── Timber ────────────────────────────────────────────────────────────
        // Plant a debug tree only in debug builds so that release builds never
        // leak log output.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("PlayAuctionApp initialised — debug=%s", BuildConfig.DEBUG)
    }
}
