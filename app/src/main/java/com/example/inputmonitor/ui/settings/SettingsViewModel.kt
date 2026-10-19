package com.example.inputmonitor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inputmonitor.core.DispatcherProvider
import com.example.inputmonitor.domain.model.MonitoringSession
import com.example.inputmonitor.domain.repository.EventRepository
import com.example.inputmonitor.domain.repository.PreferencesRepository
import com.example.inputmonitor.domain.repository.SessionRepository
import com.example.inputmonitor.domain.usecase.DeleteAllApplicationDataUseCase
import com.example.inputmonitor.domain.usecase.DeleteAllSessionsUseCase
import com.example.inputmonitor.domain.usecase.DeleteSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating user settings, logging preferences, and destructive data management actions
 * with mandatory confirmation dialogs (SRS.md §55, Architecture.md §50).
 */
class SettingsViewModel(
    private val sessionRepository: SessionRepository,
    private val eventRepository: EventRepository,
    private val preferencesRepository: PreferencesRepository,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val deleteAllSessionsUseCase: DeleteAllSessionsUseCase,
    private val deleteAllApplicationDataUseCase: DeleteAllApplicationDataUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _dialogState = MutableStateFlow(DialogState())
    private val _feedbackState = MutableStateFlow(FeedbackState())

    private val modalAndFeedbackFlow = combine(
        _dialogState,
        _feedbackState
    ) { dialog, feedback ->
        ModalAndFeedback(dialog, feedback)
    }

    private val totalEventsCountFlow = eventRepository.observeEventCounts(null)
        .map { it.total.toInt() }

    val uiState: StateFlow<SettingsUiState> = combine(
        sessionRepository.observeAllSessions(),
        preferencesRepository.isKeyboardLoggingEnabled,
        preferencesRepository.isAccessibilityLoggingEnabled,
        totalEventsCountFlow,
        modalAndFeedbackFlow
    ) { sessions, kbLogging, accLogging, eventsCount, modalAndFeedback ->
        SettingsUiState(
            sessions = sessions,
            totalEventsCount = eventsCount,
            isKeyboardLoggingEnabled = kbLogging,
            isAccessibilityLoggingEnabled = accLogging,
            isLoading = false,
            isOperating = modalAndFeedback.feedback.isOperating,
            pendingDeleteSession = modalAndFeedback.dialog.pendingDeleteSession,
            showDeleteAllSessionsDialog = modalAndFeedback.dialog.showDeleteAllSessionsDialog,
            showDeleteAllDataDialog = modalAndFeedback.dialog.showDeleteAllDataDialog,
            userMessage = modalAndFeedback.feedback.userMessage,
            error = modalAndFeedback.feedback.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState(isLoading = true)
    )

    fun setKeyboardLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            preferencesRepository.setKeyboardLoggingEnabled(enabled)
        }
    }

    fun setAccessibilityLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            preferencesRepository.setAccessibilityLoggingEnabled(enabled)
        }
    }

    fun requestDeleteSession(session: MonitoringSession) {
        _dialogState.value = _dialogState.value.copy(pendingDeleteSession = session)
    }

    fun dismissDeleteSessionDialog() {
        _dialogState.value = _dialogState.value.copy(pendingDeleteSession = null)
    }

    fun confirmDeleteSession() {
        val session = _dialogState.value.pendingDeleteSession ?: return
        _dialogState.value = _dialogState.value.copy(pendingDeleteSession = null)
        _feedbackState.value = _feedbackState.value.copy(isOperating = true, error = null)

        viewModelScope.launch(dispatchers.io) {
            deleteSessionUseCase(session.id)
                .onSuccess {
                    _feedbackState.value = FeedbackState(
                        isOperating = false,
                        userMessage = "Session \"${session.name}\" and its events were deleted."
                    )
                }
                .onFailure { t ->
                    _feedbackState.value = FeedbackState(
                        isOperating = false,
                        error = "Failed to delete session: ${t.localizedMessage ?: "Unknown error"}"
                    )
                }
        }
    }

    fun requestDeleteAllSessions() {
        _dialogState.value = _dialogState.value.copy(showDeleteAllSessionsDialog = true)
    }

    fun dismissDeleteAllSessionsDialog() {
        _dialogState.value = _dialogState.value.copy(showDeleteAllSessionsDialog = false)
    }

    fun confirmDeleteAllSessions() {
        _dialogState.value = _dialogState.value.copy(showDeleteAllSessionsDialog = false)
        _feedbackState.value = _feedbackState.value.copy(isOperating = true, error = null)

        viewModelScope.launch(dispatchers.io) {
            deleteAllSessionsUseCase()
                .onSuccess {
                    _feedbackState.value = FeedbackState(
                        isOperating = false,
                        userMessage = "All sessions and recorded events were permanently deleted."
                    )
                }
                .onFailure { t ->
                    _feedbackState.value = FeedbackState(
                        isOperating = false,
                        error = "Failed to delete all sessions: ${t.localizedMessage ?: "Unknown error"}"
                    )
                }
        }
    }

    fun requestDeleteAllData() {
        _dialogState.value = _dialogState.value.copy(showDeleteAllDataDialog = true)
    }

    fun dismissDeleteAllDataDialog() {
        _dialogState.value = _dialogState.value.copy(showDeleteAllDataDialog = false)
    }

    fun confirmDeleteAllData() {
        _dialogState.value = _dialogState.value.copy(showDeleteAllDataDialog = false)
        _feedbackState.value = _feedbackState.value.copy(isOperating = true, error = null)

        viewModelScope.launch(dispatchers.io) {
            deleteAllApplicationDataUseCase()
                .onSuccess {
                    _feedbackState.value = FeedbackState(
                        isOperating = false,
                        userMessage = "All application data and preferences have been reset."
                    )
                }
                .onFailure { t ->
                    _feedbackState.value = FeedbackState(
                        isOperating = false,
                        error = "Failed to reset application data: ${t.localizedMessage ?: "Unknown error"}"
                    )
                }
        }
    }

    fun dismissUserMessage() {
        _feedbackState.value = _feedbackState.value.copy(userMessage = null)
    }

    fun dismissError() {
        _feedbackState.value = _feedbackState.value.copy(error = null)
    }

    private data class DialogState(
        val pendingDeleteSession: MonitoringSession? = null,
        val showDeleteAllSessionsDialog: Boolean = false,
        val showDeleteAllDataDialog: Boolean = false
    )

    private data class FeedbackState(
        val isOperating: Boolean = false,
        val userMessage: String? = null,
        val error: String? = null
    )

    private data class ModalAndFeedback(
        val dialog: DialogState,
        val feedback: FeedbackState
    )

    companion object {
        fun provideFactory(
            sessionRepository: SessionRepository,
            eventRepository: EventRepository,
            preferencesRepository: PreferencesRepository,
            deleteSessionUseCase: DeleteSessionUseCase,
            deleteAllSessionsUseCase: DeleteAllSessionsUseCase,
            deleteAllApplicationDataUseCase: DeleteAllApplicationDataUseCase,
            dispatchers: DispatcherProvider
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    sessionRepository = sessionRepository,
                    eventRepository = eventRepository,
                    preferencesRepository = preferencesRepository,
                    deleteSessionUseCase = deleteSessionUseCase,
                    deleteAllSessionsUseCase = deleteAllSessionsUseCase,
                    deleteAllApplicationDataUseCase = deleteAllApplicationDataUseCase,
                    dispatchers = dispatchers
                ) as T
            }
        }
    }
}
