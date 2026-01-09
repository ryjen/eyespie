package com.micrantha.eyespie.features.onboarding.ui.genai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.components.StateRenderer
import com.micrantha.bluebell.ui.model.isBusy
import com.micrantha.bluebell.ui.model.isFailure
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.core.ui.Screen
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadAction.Done
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadUiState
import eyespie.euphrasia.generated.resources.done
import eyespie.euphrasia.generated.resources.download_failed
import eyespie.euphrasia.generated.resources.downloading
import eyespie.euphrasia.generated.resources.onboarding_genai_text
import eyespie.euphrasia.generated.resources.onboarding_genai_title
import org.jetbrains.compose.resources.stringResource

class GenAIDownloadScreen : Screen, StateRenderer<GenAiDownloadUiState> {

    @Composable
    override fun Content() {

        val model = rememberScreenModel<GenAiDownloadScreenModel>()

        val state by model.state.collectAsState()

        Render(state, model)
    }

    @Composable
    override fun Render(state: GenAiDownloadUiState, dispatch: Dispatch) {
        Box {
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

                LinearProgressIndicator(
                    progress = {
                        (state.progress / 100f).coerceIn(0f, 1f)
                    }
                )

                if (state.status.isFailure) {
                    Text(
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                            .padding(Dimensions.Padding.large),
                        text = stringResource(S.download_failed)
                    )
                } else if (state.status.isBusy) {
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
                        enabled = state.status.isBusy.not(),
                        onClick = { dispatch(Done) }) {
                        Text(stringResource(S.done))
                    }
                }
            }
        }
    }
}
