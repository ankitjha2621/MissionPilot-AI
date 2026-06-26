package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MissionPilotTheme
import com.example.ui.viewmodel.MissionPilotViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MissionPilotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MissionPilotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("splash") }
                    
                    when (currentScreen) {
                        "splash" -> SplashScreen(onTimeout = { currentScreen = "dashboard" })
                        "dashboard" -> DashboardScreen(
                            viewModel = viewModel,
                            onCreateGoalClick = { currentScreen = "create_goal" },
                            onActiveMissionClick = { currentScreen = "mission_mode" },
                            onViewReflectionsClick = { currentScreen = "reflection" }
                        )
                        "create_goal" -> CreateGoalScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "dashboard" },
                            onGoalCreated = { currentScreen = "dashboard" }
                        )
                        "mission_mode" -> MissionModeScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "dashboard" },
                            onStuckClick = { currentScreen = "rescue_mode" },
                            onCompleteSuccess = { currentScreen = "reflection" }
                        )
                        "rescue_mode" -> RescueModeScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "mission_mode" }
                        )
                        "reflection" -> ReflectionScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "dashboard" },
                            onFinished = { currentScreen = "dashboard" }
                        )
                    }
                }
            }
        }
    }
}
