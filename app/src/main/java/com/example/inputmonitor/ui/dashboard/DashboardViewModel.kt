package com.example.inputmonitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inputmonitor.domain.model.MonitoringSession
import com.example.inputmonitor.domain.model.SessionState
import com.example.inputmonitor.domain.usecase.GetActiveSessionUseCase
import com.example.inputmonitor.domain.usecase.ObserveActiveSessionUseCase
import com.example.inputmonitor.domain.usecase.ObserveSessionStateUseCase
import com.example.inputmonitor.domain.usecase.PauseMonitoringSessionUseCase
import com.example.inputmonitor.domain.usecase.ResumeMonitoringSessionUseCase
import com.example.inputmonitor.domain.usecase.StartMonitoringSessionUseCase
import com.example.inputmonitor.domain.usecase.StopMonitoringSessionUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard screen, managing session state and controls (Architecture.md §14).
 */
class DashboardViewModel(
    private val startMonitoringSessionUseCase: StartMonitoringSessionUseCase,
    private val pauseMonitoringSessionUseCase: PauseMonitoringSessionUseCase,
    private val resumeMonitoringSessionUseCase: ResumeMonitoringSessionUseCase,
    private val stopMonitoringSessionUseCase: StopMonitoringSessionUseCase,
    observeActiveSessionUseCase: ObserveActiveSessionUseCase,
    observeSessionStateUseCase: ObserveSessionStateUseCase
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = observeSessionStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionState.INACTIVE
        )

    val currentSession: StateFlow<MonitoringSession?> = observeActiveSessionUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun startSession(name: String, notes: String? = null) {
        viewModelScope.launch {
            startMonitoringSessionUseCase(name, notes)
        }
    }

    fun pauseSession() {
        viewModelScope.launch {
            pauseMonitoringSessionUseCase()
        }
    }

    fun resumeSession() {
        viewModelScope.launch {
            resumeMonitoringSessionUseCase()
        }
    }

    fun stopSession() {
        viewModelScope.launch {
            stopMonitoringSessionUseCase()
        }
    }

    companion object {
        fun provideFactory(
            startUseCase: StartMonitoringSessionUseCase,
            pauseUseCase: PauseMonitoringSessionUseCase,
            resumeUseCase: ResumeMonitoringSessionUseCase,
            stopUseCase: StopMonitoringSessionUseCase,
            observeActiveUseCase: ObserveActiveSessionUseCase,
            observeStateUseCase: ObserveSessionStateUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(
                    startUseCase,
                    pauseUseCase,
                    resumeUseCase,
                    stopUseCase,
                    observeActiveUseCase,
                    observeStateUseCase
                ) as T
            }
        }
    }
}
