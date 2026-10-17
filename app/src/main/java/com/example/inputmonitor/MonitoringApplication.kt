package com.example.inputmonitor

import android.app.Application

/**
 * Application entry point.
 *
 * Responsibilities:
 * - Initialize application-wide dependencies
 * - Initialize database (future phase)
 * - Initialize repositories (future phase)
 * - Initialize configuration providers (future phase)
 *
 * Must NOT start covert/background monitoring.
 */
class MonitoringApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Phase 0: Minimal application initialization.
        // Dependencies will be wired here in Phase 1+.
    }
}
