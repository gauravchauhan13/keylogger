package com.example.inputmonitor.data.session

import android.os.Build
import com.example.inputmonitor.core.DispatcherProvider
import com.example.inputmonitor.domain.model.MonitoringSession
import com.example.inputmonitor.domain.model.SessionState
import com.example.inputmonitor.domain.repository.PreferencesRepository
import com.example.inputmonitor.domain.repository.SessionRepository
import com.example.inputmonitor.domain.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production implementation of [SessionManager] (Architecture.md §43).
 *
 * Coordinates in-memory StateFlow observables with persistent Room storage
 * and Preferences DataStore for restart restoration.
 */
class SessionManagerImpl(
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val dispatchers: DispatcherProvider,
    private val androidVersion: String = Build.VERSION.RELEASE ?: "Unknown",
    private val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    private val appVersion: String = "0.1.0"
) : SessionManager {

    private val mutex = Mutex()

    private val _currentSession = MutableStateFlow<MonitoringSession?>(null)
    override val currentSession: StateFlow<MonitoringSession?> = _currentSession.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState.INACTIVE)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun startSession(name: String, notes: String?): MonitoringSession = withContext(dispatchers.io) {
        mutex.withLock {
            // If there's an ongoing active or paused session, stop it first
            val existing = _currentSession.value
            if (existing != null && (existing.state == SessionState.ACTIVE || existing.state == SessionState.PAUSED)) {
                val end = System.currentTimeMillis()
                sessionRepository.finishSession(existing.id, end)
            }

            val sessionId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val newSession = MonitoringSession(
                id = sessionId,
                name = name,
                startTime = now,
                endTime = null,
                state = SessionState.ACTIVE,
                androidVersion = androidVersion,
                deviceModel = deviceModel,
                appVersion = appVersion,
                notes = notes
            )

            sessionRepository.createSession(newSession)
            preferencesRepository.setActiveSessionId(sessionId)

            _currentSession.value = newSession
            _sessionState.value = SessionState.ACTIVE

            newSession
        }
    }

    override suspend fun pauseSession(): Boolean = withContext(dispatchers.io) {
        mutex.withLock {
            val current = _currentSession.value ?: return@withLock false
            if (_sessionState.value != SessionState.ACTIVE) {
                return@withLock false
            }

            val updated = current.copy(state = SessionState.PAUSED)
            sessionRepository.updateSession(updated)

            _currentSession.value = updated
            _sessionState.value = SessionState.PAUSED
            true
        }
    }

    override suspend fun resumeSession(): Boolean = withContext(dispatchers.io) {
        mutex.withLock {
            val current = _currentSession.value ?: return@withLock false
            if (_sessionState.value != SessionState.PAUSED) {
                return@withLock false
            }

            val updated = current.copy(state = SessionState.ACTIVE)
            sessionRepository.updateSession(updated)

            _currentSession.value = updated
            _sessionState.value = SessionState.ACTIVE
            true
        }
    }

    override suspend fun stopSession(): Boolean = withContext(dispatchers.io) {
        mutex.withLock {
            val current = _currentSession.value ?: return@withLock false
            if (_sessionState.value != SessionState.ACTIVE && _sessionState.value != SessionState.PAUSED) {
                return@withLock false
            }

            val end = System.currentTimeMillis()
            sessionRepository.finishSession(current.id, end)
            preferencesRepository.setActiveSessionId(null)

            val updated = current.copy(endTime = end, state = SessionState.STOPPED)
            _currentSession.value = updated
            _sessionState.value = SessionState.STOPPED
            true
        }
    }

    override suspend fun restoreSessionIfActive(): MonitoringSession? = withContext(dispatchers.io) {
        mutex.withLock {
            val savedSessionId = preferencesRepository.activeSessionId.first() ?: return@withLock null
            val session = sessionRepository.getSessionById(savedSessionId)

            if (session != null && session.state != SessionState.STOPPED) {
                _currentSession.value = session
                _sessionState.value = session.state
                session
            } else {
                preferencesRepository.setActiveSessionId(null)
                if (session != null) {
                    _currentSession.value = session
                    _sessionState.value = SessionState.STOPPED
                }
                null
            }
        }
    }
}
