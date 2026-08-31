package com.micrantha.eyespie.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Travel-spy violet brand color from the Eyespie mockups (docs/design/eyespie-app-mockups).
private val Violet = Color(0xFF5B3E8F)
private val OnViolet = Color(0xFFFFFFFF)
private val VioletContainer = Color(0xFFE9DEFF)
private val OnVioletContainer = Color(0xFF21005D)

// Friendly "Local Mode" green used for the positive offline-status accent.
val Success = Color(0xFF2E7D4F)
val OnSuccess = Color(0xFFFFFFFF)
val SuccessContainer = Color(0xFFC8EED3)
val OnSuccessContainer = Color(0xFF0B3A21)

val LightColorScheme = lightColorScheme(
    primary = Violet,
    onPrimary = OnViolet,
    primaryContainer = VioletContainer,
    onPrimaryContainer = OnVioletContainer,
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    surfaceBright = Color(0xFFFCF8FF),
    surfaceDim = Color(0xFFDED7E5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5EFF7),
    surfaceContainer = Color(0xFFEFE9F4),
    surfaceContainerHigh = Color(0xFFE9E2F0),
    surfaceContainerHighest = Color(0xFFE3DCEB),
    inverseSurface = Color(0xFF312E35),
    inverseOnSurface = Color(0xFFF4EEF8),
    inversePrimary = Color(0xFFD4BCFF),
    scrim = Color(0xFF000000),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4BCFF),
    onPrimary = Color(0xFF3A2270),
    primaryContainer = Color(0xFF523A86),
    onPrimaryContainer = Color(0xFFE9DEFF),
    secondary = Color(0xFFCBC2DC),
    onSecondary = Color(0xFF322F3D),
    secondaryContainer = Color(0xFF484454),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFE2BDC8),
    onTertiary = Color(0xFF492530),
    tertiaryContainer = Color(0xFF61404B),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF141216),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141216),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F9A),
    outlineVariant = Color(0xFF49454F),
    surfaceBright = Color(0xFF3A373F),
    surfaceDim = Color(0xFF141216),
    surfaceContainerLowest = Color(0xFF0F0E11),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF221F25),
    surfaceContainerHigh = Color(0xFF2C2930),
    surfaceContainerHighest = Color(0xFF37343B),
    inverseSurface = Color(0xFFE6E1E9),
    inverseOnSurface = Color(0xFF312E35),
    inversePrimary = Color(0xFF5B3E8F),
    scrim = Color(0xFF000000),
)
