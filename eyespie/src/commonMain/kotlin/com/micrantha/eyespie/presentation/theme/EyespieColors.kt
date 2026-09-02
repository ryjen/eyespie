package com.micrantha.eyespie.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Canonical Micrantha Lens palette.
 *
 * The original Wayfinder mockups established the travel-spy hierarchy and component language;
 * the later Micrantha Lens work established the product palette. Keep those concerns reconciled
 * here rather than carrying a second Material palette beside the brand contract.
 */
object EyespieBrandColors {
    val Field = Color(0xFFD9E3DF)
    val Petal = Color(0xFF829FC0)
    val PetalInner = Color(0xFF6F8CA8)
    val Throat = Color(0xFFEEE7CD)
    val Iris = Color(0xFFB59C69)
    val Pupil = Color(0xFF263947)
    val Ink = Color(0xFF314956)
    val White = Color(0xFFF5F5F0)
}

// Semantic positive status. This intentionally remains distinct from the brand accent because
// "local", "verified", and "found" are state, not decoration.
val Success = Color(0xFF4F755F)
val OnSuccess = Color(0xFFFFFFFF)
val SuccessContainer = Color(0xFFD7E8DD)
val OnSuccessContainer = Color(0xFF173426)

val LightColorScheme = lightColorScheme(
    primary = EyespieBrandColors.Ink,
    onPrimary = EyespieBrandColors.White,
    primaryContainer = EyespieBrandColors.Petal,
    onPrimaryContainer = EyespieBrandColors.Pupil,
    secondary = EyespieBrandColors.PetalInner,
    onSecondary = EyespieBrandColors.White,
    secondaryContainer = EyespieBrandColors.Throat,
    onSecondaryContainer = EyespieBrandColors.Ink,
    tertiary = EyespieBrandColors.Iris,
    onTertiary = EyespieBrandColors.Pupil,
    tertiaryContainer = EyespieBrandColors.Throat,
    onTertiaryContainer = EyespieBrandColors.Ink,
    error = Color(0xFF9F3D38),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF4DCD8),
    onErrorContainer = Color(0xFF49120F),
    background = EyespieBrandColors.Field,
    onBackground = EyespieBrandColors.Pupil,
    surface = EyespieBrandColors.White,
    onSurface = EyespieBrandColors.Pupil,
    surfaceVariant = EyespieBrandColors.Throat,
    onSurfaceVariant = EyespieBrandColors.Ink,
    outline = EyespieBrandColors.PetalInner,
    outlineVariant = EyespieBrandColors.Petal,
    surfaceBright = EyespieBrandColors.White,
    surfaceDim = Color(0xFFC5D1CE),
    surfaceContainerLowest = Color(0xFFFBFBF7),
    surfaceContainerLow = Color(0xFFF2F3EC),
    surfaceContainer = EyespieBrandColors.White,
    surfaceContainerHigh = Color(0xFFECEDE5),
    surfaceContainerHighest = EyespieBrandColors.Throat,
    inverseSurface = EyespieBrandColors.Pupil,
    inverseOnSurface = EyespieBrandColors.White,
    inversePrimary = EyespieBrandColors.Petal,
    surfaceTint = EyespieBrandColors.PetalInner,
    scrim = Color(0xFF000000),
)

val DarkColorScheme = darkColorScheme(
    primary = EyespieBrandColors.Petal,
    onPrimary = EyespieBrandColors.Pupil,
    primaryContainer = EyespieBrandColors.Ink,
    onPrimaryContainer = EyespieBrandColors.White,
    secondary = EyespieBrandColors.PetalInner,
    onSecondary = EyespieBrandColors.Pupil,
    secondaryContainer = Color(0xFF3A4E59),
    onSecondaryContainer = EyespieBrandColors.White,
    tertiary = EyespieBrandColors.Iris,
    onTertiary = EyespieBrandColors.Pupil,
    tertiaryContainer = Color(0xFF574C37),
    onTertiaryContainer = EyespieBrandColors.Throat,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF15232B),
    onBackground = Color(0xFFE6EEEB),
    surface = Color(0xFF1A2931),
    onSurface = Color(0xFFE6EEEB),
    surfaceVariant = Color(0xFF33454E),
    onSurfaceVariant = Color(0xFFD3DEDA),
    outline = EyespieBrandColors.Petal,
    outlineVariant = Color(0xFF526873),
    surfaceBright = Color(0xFF344750),
    surfaceDim = Color(0xFF132129),
    surfaceContainerLowest = Color(0xFF0E1B22),
    surfaceContainerLow = Color(0xFF1B2A32),
    surfaceContainer = Color(0xFF203139),
    surfaceContainerHigh = Color(0xFF2A3B43),
    surfaceContainerHighest = Color(0xFF35464E),
    inverseSurface = EyespieBrandColors.White,
    inverseOnSurface = EyespieBrandColors.Pupil,
    inversePrimary = EyespieBrandColors.Ink,
    surfaceTint = EyespieBrandColors.Petal,
    scrim = Color(0xFF000000),
)
