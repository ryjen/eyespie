package com.micrantha.eyespie.features.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.micrantha.eyespie.core.PreviewContext
import com.micrantha.eyespie.domain.entities.ModelFile

@Preview
@Composable
fun OnboardingScreenPreview() = PreviewContext(
    OnboardingUiState(
        page = OnboardingPage.GenAI,
        isBusy = true,
        isError = true,
        models = listOf(
            ModelFile(
                downloadUrl = "harst",
                name = "harst",
                slug = "arst"
            )
        ),
        selectedModel = null
    )
) {
    OnboardingScreen()
}
