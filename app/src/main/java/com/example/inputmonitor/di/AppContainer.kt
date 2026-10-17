package com.example.inputmonitor.di

import android.content.Context
import com.example.inputmonitor.core.DefaultDispatcherProvider
import com.example.inputmonitor.core.DispatcherProvider

import com.example.inputmonitor.data.database.AppDatabase
import com.example.inputmonitor.data.database.DatabaseProvider
import com.example.inputmonitor.data.preferences.PreferencesRepositoryImpl
import com.example.inputmonitor.data.repository.EventRepositoryImpl
import com.example.inputmonitor.data.repository.SessionRepositoryImpl
import com.example.inputmonitor.data.session.SessionGateImpl
import com.example.inputmonitor.data.session.SessionManagerImpl
import com.example.inputmonitor.domain.repository.EventRepository
import com.example.inputmonitor.domain.repository.PreferencesRepository
import com.example.inputmonitor.domain.repository.SessionRepository
import com.example.inputmonitor.domain.session.SessionGate
import com.example.inputmonitor.domain.session.SessionManager
import com.example.inputmonitor.domain.usecase.GetActiveSessionUseCase
import com.example.inputmonitor.domain.usecase.ObserveActiveSessionUseCase
import com.example.inputmonitor.domain.usecase.ObserveSessionStateUseCase
import com.example.inputmonitor.domain.usecase.PauseMonitoringSessionUseCase
import com.example.inputmonitor.domain.usecase.ResumeMonitoringSessionUseCase
import com.example.inputmonitor.domain.usecase.StartMonitoringSessionUseCase
import com.example.inputmonitor.domain.usecase.StopMonitoringSessionUseCase
import com.example.inputmonitor.domain.privacy.PrivacyFilter
import com.example.inputmonitor.domain.privacy.PrivacyFilterImpl
import com.example.inputmonitor.domain.privacy.SensitiveInputDetector
import com.example.inputmonitor.domain.privacy.SensitiveInputDetectorImpl
import com.example.inputmonitor.platform.ime.analysis.EditorInfoAnalyzer
import com.example.inputmonitor.platform.ime.analysis.EditorInfoAnalyzerImpl

/**
 * Application-level dependency container implementing manual DI (DEC-004).
 *
 * Provides single-point initialization and access to core application singletons
 * without Hilt runtime overhead or annotation processing costs.
 */
interface AppContainer {
    val context: Context
    val dispatchers: DispatcherProvider
    val database: AppDatabase
    val sessionRepository: SessionRepository
    val eventRepository: EventRepository
    val preferencesRepository: PreferencesRepository
    val sessionManager: SessionManager
    val sessionGate: SessionGate
    val editorInfoAnalyzer: EditorInfoAnalyzer
    val sensitiveInputDetector: SensitiveInputDetector
    val privacyFilter: PrivacyFilter
    val startMonitoringSessionUseCase: StartMonitoringSessionUseCase
    val pauseMonitoringSessionUseCase: PauseMonitoringSessionUseCase
    val resumeMonitoringSessionUseCase: ResumeMonitoringSessionUseCase
    val stopMonitoringSessionUseCase: StopMonitoringSessionUseCase
    val getActiveSessionUseCase: GetActiveSessionUseCase
    val observeActiveSessionUseCase: ObserveActiveSessionUseCase
    val observeSessionStateUseCase: ObserveSessionStateUseCase
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

    override val database: AppDatabase by lazy {
        DatabaseProvider.createEncryptedDatabase(context)
    }

    override val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(database.monitoringSessionDao(), dispatchers)
    }

    override val eventRepository: EventRepository by lazy {
        EventRepositoryImpl(database.interactionEventDao(), dispatchers)
    }

    override val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepositoryImpl(context, dispatchers)
    }

    override val sessionManager: SessionManager by lazy {
        SessionManagerImpl(sessionRepository, preferencesRepository, dispatchers)
    }

    override val sessionGate: SessionGate by lazy {
        SessionGateImpl(sessionManager)
    }

    override val editorInfoAnalyzer: EditorInfoAnalyzer by lazy {
        EditorInfoAnalyzerImpl()
    }

    override val sensitiveInputDetector: SensitiveInputDetector by lazy {
        SensitiveInputDetectorImpl()
    }

    override val privacyFilter: PrivacyFilter by lazy {
        PrivacyFilterImpl()
    }

    override val startMonitoringSessionUseCase: StartMonitoringSessionUseCase by lazy {
        StartMonitoringSessionUseCase(sessionManager)
    }

    override val pauseMonitoringSessionUseCase: PauseMonitoringSessionUseCase by lazy {
        PauseMonitoringSessionUseCase(sessionManager)
    }

    override val resumeMonitoringSessionUseCase: ResumeMonitoringSessionUseCase by lazy {
        ResumeMonitoringSessionUseCase(sessionManager)
    }

    override val stopMonitoringSessionUseCase: StopMonitoringSessionUseCase by lazy {
        StopMonitoringSessionUseCase(sessionManager)
    }

    override val getActiveSessionUseCase: GetActiveSessionUseCase by lazy {
        GetActiveSessionUseCase(sessionManager)
    }

    override val observeActiveSessionUseCase: ObserveActiveSessionUseCase by lazy {
        ObserveActiveSessionUseCase(sessionManager)
    }

    override val observeSessionStateUseCase: ObserveSessionStateUseCase by lazy {
        ObserveSessionStateUseCase(sessionManager)
    }
}
