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
    navController: NavHostController = rememberNavController()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.example.inputmonitor.MonitoringApplication).container

    val setupViewModel: com.example.inputmonitor.ui.setup.SetupViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.inputmonitor.ui.setup.SetupViewModel.provideFactory(
            setupStateManager = container.setupStateManager,
            permissionCoordinator = container.permissionCoordinator,
            preferencesRepository = container.preferencesRepository
        )
    )

    val setupState by setupViewModel.setupState.collectAsState()
    val startDestination = if (setupState.isOnboardingCompleted) {
        Screen.Dashboard.route
    } else {
        Screen.Onboarding.route
    }

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
                setupState = setupState,
                onOpenAccessibilitySettings = { setupViewModel.openAccessibilitySettings(context) },
                onOpenKeyboardSettings = { setupViewModel.openKeyboardSettings(context) },
                onShowKeyboardPicker = { setupViewModel.showKeyboardPicker(context) },
                onCompleteSetup = {
                    setupViewModel.completeSetup {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
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
                    observeStateUseCase = container.observeSessionStateUseCase,
                    setupStateManager = container.setupStateManager,
                    eventRepository = container.eventRepository
                )
            )

            val uiState by dashboardViewModel.uiState.collectAsState()

            DashboardScreen(
                uiState = uiState,
                onStartSession = { name, notes -> dashboardViewModel.startSession(name, notes) },
                onPauseSession = { dashboardViewModel.pauseSession() },
                onResumeSession = { dashboardViewModel.resumeSession() },
                onStopSession = { dashboardViewModel.stopSession() },
                onRefreshSetup = { dashboardViewModel.refreshSetup() },
                onNavigateToEvents = {
                    navController.navigate(Screen.Events.route)
                },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSetup = {
                    navController.navigate(Screen.Setup.route)
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
