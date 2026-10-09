package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MoisturTheme
import com.example.viewmodel.MoisturViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    DASHBOARD,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MoisturViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }

            // Lifecycle observer to pause polling when backgrounded and resume when foregrounded
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                            viewModel.setAppForeground(true)
                        }
                        Lifecycle.Event.ON_STOP -> {
                            viewModel.setAppForeground(false)
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Listen for feedback event messages (pump actions, errors)
            LaunchedEffect(Unit) {
                viewModel.eventMessage.collectLatest { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            MoisturTheme(themeMode = uiState.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.SPLASH -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        currentScreen = AppScreen.DASHBOARD
                                    }
                                )
                            }

                            AppScreen.DASHBOARD -> {
                                DashboardScreen(
                                    uiState = uiState,
                                    snackbarHostState = snackbarHostState,
                                    onPumpOn = { viewModel.setPump(true) },
                                    onPumpOff = { viewModel.setPump(false) },
                                    onManualRefresh = {
                                        coroutineScope.launch {
                                            viewModel.refreshData(force = true)
                                        }
                                    },
                                    onNavigateToSettings = {
                                        viewModel.clearTestConnectionState()
                                        currentScreen = AppScreen.SETTINGS
                                    }
                                )
                            }

                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    uiState = uiState,
                                    onBack = { currentScreen = AppScreen.DASHBOARD },
                                    onSaveIp = { viewModel.updateEsp32Ip(it) },
                                    onTestConnection = { viewModel.testConnection(it) },
                                    onSelectFrequency = { viewModel.updateFrequency(it) },
                                    onSelectTheme = { viewModel.updateTheme(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
