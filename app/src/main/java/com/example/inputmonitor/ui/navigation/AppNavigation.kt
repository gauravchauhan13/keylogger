package com.example.inputmonitor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inputmonitor.ui.dashboard.DashboardScreen
import com.example.inputmonitor.ui.events.EventsScreen
import com.example.inputmonitor.ui.onboarding.OnboardingScreen
import com.example.inputmonitor.ui.settings.SettingsScreen
import com.example.inputmonitor.ui.setup.SetupScreen
import com.example.inputmonitor.ui.statistics.StatisticsScreen

/**
 * Top-level application navigation graph (Architecture.md §13).
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToSetup = {
                    navController.navigate(Screen.Setup.route)
                }
            )
        }

        composable(Screen.Setup.route) {
            SetupScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val container = (context.applicationContext as com.example.inputmonitor.MonitoringApplication).container

            val dashboardViewModel: com.example.inputmonitor.ui.dashboard.DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.inputmonitor.ui.dashboard.DashboardViewModel.provideFactory(
                    startUseCase = container.startMonitoringSessionUseCase,
                    pauseUseCase = container.pauseMonitoringSessionUseCase,
                    resumeUseCase = container.resumeMonitoringSessionUseCase,
                    stopUseCase = container.stopMonitoringSessionUseCase,
                    observeActiveUseCase = container.observeActiveSessionUseCase,
                    observeStateUseCase = container.observeSessionStateUseCase
                )
            )

            val sessionState by dashboardViewModel.sessionState.collectAsState()
            val currentSession by dashboardViewModel.currentSession.collectAsState()

            DashboardScreen(
                sessionState = sessionState,
                currentSession = currentSession,
                onStartSession = { name, notes -> dashboardViewModel.startSession(name, notes) },
                onPauseSession = { dashboardViewModel.pauseSession() },
                onResumeSession = { dashboardViewModel.resumeSession() },
                onStopSession = { dashboardViewModel.stopSession() },
                onNavigateToEvents = {
                    navController.navigate(Screen.Events.route)
                },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Events.route) {
            EventsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
