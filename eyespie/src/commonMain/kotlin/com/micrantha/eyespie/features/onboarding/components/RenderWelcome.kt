package com.micrantha.eyespie.features.onboarding.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.app.EyesPie
import com.micrantha.eyespie.features.onboarding.entities.OnboardingUiState

@Composable
fun BoxScope.RenderWelcome(state: OnboardingUiState, dispatch: Dispatch) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.align(Alignment.Center)
            .padding(Dimensions.Padding.large),
    ) {
        Text(
            text = "Welcome to Eyespie",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Icon(
            imageVector = EyesPie.defaultIcon,
            modifier = Modifier.size(Dimensions.List.placeholder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
