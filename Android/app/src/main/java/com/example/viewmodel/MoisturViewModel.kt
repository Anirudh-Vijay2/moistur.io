package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ConnectionStatus
import com.example.model.MoistureLevel
import com.example.model.TestConnectionState
import com.example.model.ThemeMode
import com.example.model.UpdateFrequency
import com.example.network.ESP32Client
import com.example.preferences.AppPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class MoisturUiState(
    val moisture: Int = 50,
    val pumpOn: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    val isPumpActionPending: Boolean = false,
    val esp32Ip: String = AppPreferences.DEFAULT_IP,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val updateFrequency: UpdateFrequency = UpdateFrequency.TWO_SECONDS,
    val testConnectionState: TestConnectionState = TestConnectionState.Idle,
    val lastUpdatedTimestamp: Long? = null,
    val statusMessage: String? = null
) {
    val moistureLevel: MoistureLevel
        get() = MoistureLevel.fromPercentage(moisture)
}

class MoisturViewModel(
    application: Application,
    private val client: ESP32Client
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, ESP32Client())

    private val preferences = AppPreferences(application.applicationContext)

    private val _uiState = MutableStateFlow(
        MoisturUiState(
            moisture = preferences.lastMoisture,
            pumpOn = preferences.lastPumpState,
            esp32Ip = preferences.esp32Ip,
            themeMode = preferences.themeMode,
            updateFrequency = preferences.updateFrequency
        )
    )
    val uiState: StateFlow<MoisturUiState> = _uiState.asStateFlow()

    private val _eventMessage = MutableSharedFlow<String>()
    val eventMessage: SharedFlow<String> = _eventMessage.asSharedFlow()

    private var pollingJob: Job? = null
    private val isRequestInProgress = AtomicBoolean(false)
    private var isAppInForeground = true

    init {
        startPolling()
    }

    fun setAppForeground(isInForeground: Boolean) {
        if (isAppInForeground == isInForeground) return
        isAppInForeground = isInForeground
        if (isInForeground) {
            startPolling()
            // Immediate refresh on return to foreground
            viewModelScope.launch { refreshData(force = true) }
        } else {
            stopPolling()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isAppInForeground) {
                refreshData(force = false)
                val interval = _uiState.value.updateFrequency.intervalMs
                delay(interval)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun refreshData(force: Boolean = false) {
        if (!force && !isAppInForeground) return
        if (!isRequestInProgress.compareAndSet(false, true)) {
            // Avoid overlapping requests
            return
        }

        try {
            val currentIp = _uiState.value.esp32Ip
            val result = client.fetchData(currentIp)
            result.onSuccess { data ->
                preferences.lastMoisture = data.moisture
                preferences.lastPumpState = data.pump
                _uiState.update { current ->
                    current.copy(
                        moisture = data.moisture,
                        pumpOn = data.pump,
                        connectionStatus = ConnectionStatus.CONNECTED,
                        lastUpdatedTimestamp = System.currentTimeMillis(),
                        statusMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        // Note: Keep displaying last successfully received moisture value!
                        statusMessage = error.message ?: "Unable to connect to ESP32"
                    )
                }
            }
        } finally {
            isRequestInProgress.set(false)
        }
    }

    fun setPump(turnOn: Boolean) {
        if (_uiState.value.isPumpActionPending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPumpActionPending = true) }
            val currentIp = _uiState.value.esp32Ip
            val result = client.setPump(currentIp, turnOn)

            result.onSuccess { newState ->
                preferences.lastPumpState = newState
                _uiState.update {
                    it.copy(
                        pumpOn = newState,
                        isPumpActionPending = false,
                        connectionStatus = ConnectionStatus.CONNECTED,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }
                _eventMessage.emit(if (newState) "Pump turned ON" else "Pump turned OFF")
                // Trigger immediate data refresh to synchronize all sensor and pump states
                refreshData(force = true)
            }.onFailure { error ->
                _uiState.update { it.copy(isPumpActionPending = false) }
                val errorMsg = error.message ?: "Failed to change pump state"
                _eventMessage.emit(errorMsg)
            }
        }
    }

    fun updateEsp32Ip(rawIp: String) {
        val sanitized = client.sanitizeIp(rawIp)
        preferences.esp32Ip = sanitized
        _uiState.update {
            it.copy(
                esp32Ip = sanitized,
                testConnectionState = TestConnectionState.Idle
            )
        }
        // Restart polling loop with the new IP
        startPolling()
    }

    fun testConnection(ipToTest: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(testConnectionState = TestConnectionState.Testing) }
            val sanitized = client.sanitizeIp(ipToTest)
            val result = client.fetchData(sanitized)

            result.onSuccess { data ->
                // Update local storage and UI state
                preferences.esp32Ip = sanitized
                preferences.lastMoisture = data.moisture
                preferences.lastPumpState = data.pump
                _uiState.update {
                    it.copy(
                        esp32Ip = sanitized,
                        moisture = data.moisture,
                        pumpOn = data.pump,
                        connectionStatus = ConnectionStatus.CONNECTED,
                        lastUpdatedTimestamp = System.currentTimeMillis(),
                        testConnectionState = TestConnectionState.Success
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        testConnectionState = TestConnectionState.Failed(
                            error.message ?: "ESP32 Not Connected"
                        )
                    )
                }
            }
        }
    }

    fun clearTestConnectionState() {
        _uiState.update { it.copy(testConnectionState = TestConnectionState.Idle) }
    }

    fun updateFrequency(frequency: UpdateFrequency) {
        preferences.updateFrequency = frequency
        _uiState.update { it.copy(updateFrequency = frequency) }
        startPolling()
    }

    fun updateTheme(themeMode: ThemeMode) {
        preferences.themeMode = themeMode
        _uiState.update { it.copy(themeMode = themeMode) }
    }
}
