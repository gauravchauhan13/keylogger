package com.example.inputmonitor.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.inputmonitor.domain.model.MonitoringSession
import com.example.inputmonitor.domain.model.SessionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main application dashboard screen (Phase 3 Monitoring Sessions / Phase 11 full).
 * Displays session lifecycle state and controls: ACTIVE, PAUSED, STOPPED, INACTIVE.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    sessionState: SessionState,
    currentSession: MonitoringSession?,
    onStartSession: (String, String?) -> Unit,
    onPauseSession: () -> Unit,
    onResumeSession: () -> Unit,
    onStopSession: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (sessionState) {
                        SessionState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                        SessionState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer
                        SessionState.STOPPED, SessionState.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monitoring Session",
                            style = MaterialTheme.typography.titleMedium
                        )
                        SessionStateBadge(state = sessionState)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentSession != null) {
                        Text(
                            text = "Session: ${currentSession.name}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        Text(
                            text = "Started: ${timeFormat.format(Date(currentSession.startTime))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (currentSession.endTime != null) {
                            Text(
                                text = "Ended: ${timeFormat.format(Date(currentSession.endTime))}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Text(
                            text = "No monitoring session running. Tap below to start monitoring.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Session Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (sessionState) {
                            SessionState.INACTIVE, SessionState.STOPPED -> {
                                Button(
                                    onClick = { showStartDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "Start Session")
                                }
                            }
                            SessionState.ACTIVE -> {
                                OutlinedButton(
                                    onClick = onPauseSession,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Pause")
                                }
                                Button(
                                    onClick = onStopSession,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Stop")
                                }
                            }
                            SessionState.PAUSED -> {
                                Button(
                                    onClick = onResumeSession,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Resume")
                                }
                                OutlinedButton(
                                    onClick = onStopSession,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Stop")
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Navigation",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToEvents,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Events")
                }
                Button(
                    onClick = onNavigateToStatistics,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Statistics")
                }
            }

            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Settings")
            }
        }
    }

    if (showStartDialog) {
        var sessionName by remember {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            mutableStateOf("Session_$dateStr")
        }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text(text = "Start Monitoring Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = { sessionName = it },
                        label = { Text("Session Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sessionName.isNotBlank()) {
                            onStartSession(sessionName, notes.ifBlank { null })
                            showStartDialog = false
                        }
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SessionStateBadge(state: SessionState) {
    val (color, text) = when (state) {
        SessionState.ACTIVE -> Color(0xFF2E7D32) to "● ACTIVE"
        SessionState.PAUSED -> Color(0xFFE65100) to "⏸ PAUSED"
        SessionState.STOPPED -> Color(0xFF546E7A) to "■ STOPPED"
        SessionState.INACTIVE -> Color(0xFF757575) to "○ INACTIVE"
    }

    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge
    )
}
