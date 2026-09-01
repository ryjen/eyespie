package com.micrantha.eyespie.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object EyespieBrandColors {
    val Field = Color(0xFFD9E3DF)
    val Petal = Color(0xFF829FC0)
    val PetalInner = Color(0xFF6F8CA8)
    val Throat = Color(0xFFEEE7CD)
    val Iris = Color(0xFFB59C69)
    val Pupil = Color(0xFF263947)
    val Ink = Color(0xFF314956)
    val White = Color(0xFFF5F5F0)
}

private val EyespieColorScheme = lightColorScheme(
    primary = EyespieBrandColors.Ink,
    onPrimary = EyespieBrandColors.White,
    primaryContainer = EyespieBrandColors.Field,
    onPrimaryContainer = EyespieBrandColors.Pupil,
    secondary = EyespieBrandColors.Pupil,
    onSecondary = EyespieBrandColors.White,
    secondaryContainer = EyespieBrandColors.Throat,
    onSecondaryContainer = EyespieBrandColors.Ink,
    tertiary = EyespieBrandColors.Iris,
    onTertiary = EyespieBrandColors.Pupil,
    tertiaryContainer = EyespieBrandColors.Throat,
    onTertiaryContainer = EyespieBrandColors.Ink,
    background = EyespieBrandColors.Field,
    onBackground = EyespieBrandColors.Pupil,
    surface = EyespieBrandColors.White,
    onSurface = EyespieBrandColors.Pupil,
    surfaceVariant = EyespieBrandColors.Throat,
    onSurfaceVariant = EyespieBrandColors.Ink,
    outline = EyespieBrandColors.PetalInner,
    outlineVariant = EyespieBrandColors.Petal,
    inverseSurface = EyespieBrandColors.Pupil,
    inverseOnSurface = EyespieBrandColors.White,
    inversePrimary = EyespieBrandColors.Petal,
    surfaceTint = EyespieBrandColors.PetalInner,
)

@Composable
internal fun EyespieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EyespieColorScheme,
        content = content,
    )
}
