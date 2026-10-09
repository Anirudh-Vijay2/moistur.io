package com.example.model

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED
}

enum class MoistureLevel(val label: String) {
    VERY_DRY("Very Dry"),
    DRY("Dry"),
    NORMAL("Normal"),
    WET("Wet");

    companion object {
        fun fromPercentage(percentage: Int): MoistureLevel {
            return when {
                percentage < 5 -> VERY_DRY
                percentage < 30 -> DRY
                percentage <= 70 -> NORMAL
                else -> WET
            }
        }
    }
}

enum class ThemeMode(val title: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

enum class UpdateFrequency(val label: String, val intervalMs: Long) {
    ONE_SECOND("1 second", 1000L),
    TWO_SECONDS("2 seconds", 2000L),
    FIVE_SECONDS("5 seconds", 5000L);

    companion object {
        fun fromInterval(intervalMs: Long): UpdateFrequency {
            return entries.firstOrNull { it.intervalMs == intervalMs } ?: TWO_SECONDS
        }
    }
}

data class ESP32Data(
    val moisture: Int,
    val pump: Boolean
)

sealed interface TestConnectionState {
    data object Idle : TestConnectionState
    data object Testing : TestConnectionState
    data object Success : TestConnectionState
    data class Failed(val message: String) : TestConnectionState
}
