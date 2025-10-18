package com.micrantha.eyespie.features.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.micrantha.eyespie.core.PreviewContext
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingUiState

@Preview
@Composable
fun OnboardingScreenPreview() = PreviewContext(
    OnboardingUiState(
        page = OnboardingPage.GenAI,
        isBusy = true,
        isError = true,
        models = listOf(
            OnboardingUiState.Model(
                "test",
                false
            ),
            OnboardingUiState.Model(
                "abc",
                true
            )
        )
    )
) {
    OnboardingScreen()
}
