package com.micrantha.eyespie.features.onboarding.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction
import com.micrantha.eyespie.features.onboarding.entities.OnboardingUiState
import eyespie.euphrasia.generated.resources.download_failed
import eyespie.euphrasia.generated.resources.downloading
import eyespie.euphrasia.generated.resources.no
import eyespie.euphrasia.generated.resources.onboarding_genai_text
import eyespie.euphrasia.generated.resources.onboarding_genai_title
import eyespie.euphrasia.generated.resources.yes
import org.jetbrains.compose.resources.stringResource

@Composable
fun BoxScope.RenderGenAI(state: OnboardingUiState, dispatch: Dispatch) {
    Text(
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.TopCenter).padding(Dimensions.screen)
            .padding(top = Dimensions.Padding.large),
        text = stringResource(S.onboarding_genai_title)
    )
    Column(
        modifier = Modifier.align(Alignment.Center).padding(top = Dimensions.Padding.large),
    ) {
        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
                .padding(Dimensions.Padding.large),
            text = stringResource(S.onboarding_genai_text)
        )

        if (state.isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .sizeIn(48.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.Padding.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(state.models) { model ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            if (model.isSelected) 3.dp else 1.dp,
                            if (model.isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        ),
                        onClick = { dispatch(OnboardingAction.SelectModel(model.name)) }
                    ) {
                        Text(
                            model.name
                        )
                    }
                }
            }
        }

        if (state.isError) {
            Text(
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
                    .padding(Dimensions.Padding.large),
                text = stringResource(S.download_failed)
            )
        } else if (state.isBusy) {
            Text(
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
                    .padding(Dimensions.Padding.large),
                text = stringResource(S.downloading)
            )
        } else {
            Spacer(Modifier.heightIn(with(LocalDensity.current) { 12.sp.toDp() }))
        }

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(Dimensions.Padding.large),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                enabled = state.isBusy.not(),
                onClick = {
                    dispatch(OnboardingAction.NextPage)
                }) {
                Text(stringResource(S.no))
            }
            OutlinedButton(
                enabled = state.isBusy.not() && state.isSelected,
                onClick = { dispatch(OnboardingAction.Download) }) {
                Text(stringResource(S.yes))
            }
        }
    }
}
