package com.example.inputmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.inputmonitor.ui.navigation.AppNavigation
import com.example.inputmonitor.ui.theme.InputMonitorTheme

/**
 * Main host activity.
 *
 * Hosts the Compose navigation graph (Architecture.md §12):
 * Onboarding → Setup → Dashboard ↔ Events / Statistics / Settings
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InputMonitorTheme {
                AppNavigation()
            }
        }
    }
}
