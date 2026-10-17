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

    lateinit var container: com.example.inputmonitor.di.AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = com.example.inputmonitor.di.DefaultAppContainer(applicationContext)
    }
}
