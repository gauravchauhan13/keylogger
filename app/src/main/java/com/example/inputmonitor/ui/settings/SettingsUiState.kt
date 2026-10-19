package com.example.inputmonitor.ui.settings

import com.example.inputmonitor.domain.model.MonitoringSession

/**
 * UI state for the Settings and Data Management screen (Architecture.md §50, SRS.md §55).
 */
data class SettingsUiState(
    val sessions: List<MonitoringSession> = emptyList(),
    val totalEventsCount: Int = 0,
    val isKeyboardLoggingEnabled: Boolean = true,
    val isAccessibilityLoggingEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isOperating: Boolean = false,
    val pendingDeleteSession: MonitoringSession? = null,
    val showDeleteAllSessionsDialog: Boolean = false,
    val showDeleteAllDataDialog: Boolean = false,
    val userMessage: String? = null,
    val error: String? = null
)
