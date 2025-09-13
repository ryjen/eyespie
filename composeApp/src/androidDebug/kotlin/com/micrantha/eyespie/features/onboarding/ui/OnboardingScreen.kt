package com.micrantha.eyespie.features.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.micrantha.eyespie.core.PreviewContext

@Preview
@Composable
fun OnboardingScreenPreview() = PreviewContext(
    OnboardingUiState(
        page = 0
    )
) {
    OnboardingScreen()
}
