package com.micrantha.eyespie.ui

import androidx.compose.runtime.Composable
import com.micrantha.eyespie.presentation.theme.EyespieTheme as CanonicalEyespieTheme

/**
 * Compatibility wrapper for callers that still live under the legacy `ui` package.
 *
 * The canonical application theme is presentation.theme.EyespieTheme; keeping this wrapper
 * prevents AppUnavailable and other transitional callers from creating a second color system.
 */
@Composable
internal fun EyespieTheme(content: @Composable () -> Unit) {
    CanonicalEyespieTheme(content = content)
}
