package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    secondary = NeonGold,
    tertiary = ElectricTeal,
    background = CyberNavy,
    surface = CyberSlate,
    error = EnergyRed,
    onPrimary = CyberNavy,
    onSecondary = CyberNavy,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force a premium high-tech dark theme for electricity load manager
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
