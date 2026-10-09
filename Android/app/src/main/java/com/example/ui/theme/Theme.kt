package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = DarkMoisturGreen,
    onPrimary = DarkBackground,
    primaryContainer = DarkMoisturGreenContainer,
    onPrimaryContainer = DarkMoisturGreen,
    secondary = DarkMoisturBlue,
    onSecondary = DarkBackground,
    secondaryContainer = DarkMoisturBlueContainer,
    onSecondaryContainer = DarkMoisturBlue,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = MoisturGreenPrimary,
    onPrimary = LightSurface,
    primaryContainer = MoisturGreenContainerLight,
    onPrimaryContainer = MoisturGreenOnContainerLight,
    secondary = MoisturBlueAccent,
    onSecondary = LightSurface,
    secondaryContainer = MoisturBlueContainerLight,
    onSecondaryContainer = MoisturBlueOnContainerLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant
)

@Composable
fun MoisturTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
