package com.micrantha.eyespie.features.onboarding

import androidx.compose.runtime.Composable

/**
 * Explicit, optional camera permission step. The OS dialog is only requested after the user
 * selects Enable camera; skipping onboarding does not grant or permanently deny permission.
 */
@Composable
expect fun OnboardingCameraPermission()
