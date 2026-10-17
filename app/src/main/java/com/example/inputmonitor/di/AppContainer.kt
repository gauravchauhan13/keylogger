package com.example.inputmonitor.di

import android.content.Context
import com.example.inputmonitor.core.DefaultDispatcherProvider
import com.example.inputmonitor.core.DispatcherProvider

/**
 * Application-level dependency container implementing manual DI (DEC-004).
 *
 * Provides single-point initialization and access to core application singletons
 * without Hilt runtime overhead or annotation processing costs.
 */
interface AppContainer {
    val context: Context
    val dispatchers: DispatcherProvider
}

/**
 * Default implementation of [AppContainer] initialized in [com.example.inputmonitor.MonitoringApplication].
 */
class DefaultAppContainer(
    override val context: Context
) : AppContainer {

    override val dispatchers: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }
}
