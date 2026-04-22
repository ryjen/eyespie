package com.micrantha.eyespie.features.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.micrantha.eyespie.core.PreviewContext
import com.micrantha.eyespie.features.onboarding.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.entities.OnboardingUiState

@Preview(
    showBackground = true,
    backgroundColor = 0xffffff
)
@Composable
fun OnboardingScreenPreview() = PreviewContext(
    OnboardingUiState(
        page = OnboardingPage.GenAI,
        isBusy = false,
        isError = true,
        isSelected = false,
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
