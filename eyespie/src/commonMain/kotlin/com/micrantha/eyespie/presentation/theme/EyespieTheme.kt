package com.micrantha.eyespie.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Semantic success/positive accent drawn from the mockups' friendly "Local Mode"
// green. Material 3 has no success role, so we expose it as an extended color
// alongside the themed colorScheme.
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = Success,
        onSuccess = OnSuccess,
        successContainer = SuccessContainer,
        onSuccessContainer = OnSuccessContainer,
    )
}

val extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

@Composable
fun EyespieTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = ExtendedColors(
        success = Success,
        onSuccess = OnSuccess,
        successContainer = SuccessContainer,
        onSuccessContainer = OnSuccessContainer,
    )

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EyespieTypography,
            shapes = EyespieShapes,
            content = content,
        )
    }
}
