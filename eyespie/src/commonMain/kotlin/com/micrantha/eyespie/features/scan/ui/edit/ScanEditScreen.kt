package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.components.StateRenderer
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.core.ui.Screen
import com.micrantha.eyespie.features.scan.components.ScannedClues
import com.micrantha.eyespie.features.scan.entities.ClueAuthoringMode
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.GenerateClues
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Init
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Retry
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.UpdateManualAnswer
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.UpdateManualClue
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.UseGeneratedAuthoring
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.UseManualAuthoring
import com.micrantha.eyespie.features.scan.entities.ScanEditParams
import com.micrantha.eyespie.features.scan.entities.ScanEditUiState
import com.micrantha.eyespie.generated.resources.clue_authoring_prompt
import com.micrantha.eyespie.generated.resources.done
import com.micrantha.eyespie.generated.resources.generate_clues
import com.micrantha.eyespie.generated.resources.generated_clues_unavailable
import com.micrantha.eyespie.generated.resources.loading_error
import com.micrantha.eyespie.generated.resources.manual_answer
import com.micrantha.eyespie.generated.resources.manual_clue
import com.micrantha.eyespie.generated.resources.new_thing
import com.micrantha.eyespie.generated.resources.use_generated_clues
import com.micrantha.eyespie.generated.resources.write_clue_manually
import org.jetbrains.compose.resources.stringResource

data class ScanEditScreen(
    private val params: ScanEditParams
) : Screen, StateRenderer<ScanEditUiState> {

    @Composable
    override fun Content() {
        val screenModel: ScanEditScreenModel = rememberScreenModel()
        val title = stringResource(S.new_thing)

        LaunchedEffect(title) {
            screenModel.dispatch(Init(params))
        }

        val state by screenModel.state.collectAsState()
        Render(state, screenModel)
    }

    @Composable
    override fun Render(state: ScanEditUiState, dispatch: Dispatch) {
        Box(modifier = Modifier.fillMaxSize()) {
            state.image?.let {
                Image(
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.align(Alignment.Center).fillMaxSize(),
                    painter = it,
                    contentDescription = null,
                )
            }

<<<<<<< Updated upstream
            Box(
                modifier = Modifier.align(Alignment.Center).fillMaxSize().padding(Dimensions.screen)
            ) {
                when {
                    state.isBusy -> Loading()
                    state.isError -> Error(dispatch)
                    else -> Authoring(state, dispatch)
||||||| Stash base
            Spacer(Modifier.height(Dimensions.content))

            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimensions.progress)
                )
            } else if (state.isError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(Dimensions.Padding.small))
                    Text(
                        textAlign = TextAlign.Center,
                        text = stringResource(S.loading_error)
                    )
                    FilledIconButton(
                        onClick = { dispatch(Retry) },
                    ) {
                        Icon(
                            Icons.Default.Refresh, null
                        )
                    }
=======
            Spacer(Modifier.height(Dimensions.content))

            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimensions.progress)
                )
            } else if (state.isError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(Dimensions.Padding.small))
                    Text(
                        textAlign = TextAlign.Center,
                        text = stringResource(S.loading_error)
                    )
                    state.errorMessage?.let {
                        Spacer(Modifier.height(Dimensions.Padding.small))
                        Text(
                            textAlign = TextAlign.Center,
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    FilledIconButton(
                        onClick = { dispatch(Retry) },
                    ) {
                        Icon(
                            Icons.Default.Refresh, null
                        )
                    }
>>>>>>> Stashed changes
                }
            }

            ElevatedButton(
                modifier = Modifier.align(Alignment.BottomCenter),
                enabled = state.enabled,
                onClick = { dispatch(SaveScanEdit) },
            ) {
                Text(stringResource(S.done))
            }
            Spacer(Modifier.height(Dimensions.screen))
        }
    }

    @Composable
    private fun BoxScope.Loading() {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center).background(
                Color.Gray.copy(alpha = 0.5f),
                RoundedCornerShape(Dimensions.Border.medium)
            ).padding(Dimensions.content).size(Dimensions.progress)
        )
    }

    @Composable
    private fun BoxScope.Error(dispatch: Dispatch) {
        Column(
            Modifier.align(Alignment.Center).background(
                Color.Gray.copy(alpha = 0.5f),
                RoundedCornerShape(Dimensions.Border.medium)
            ).padding(Dimensions.content),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.heightIn(Dimensions.Padding.small))
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(S.loading_error),
            )
            FilledIconButton(onClick = { dispatch(Retry) }) {
                Icon(Icons.Default.Refresh, null)
            }
        }
    }

    @Composable
    private fun BoxScope.Authoring(state: ScanEditUiState, dispatch: Dispatch) {
        when (state.authoringMode) {
            ClueAuthoringMode.CHOOSE -> AuthoringChoice(dispatch)
            ClueAuthoringMode.GENERATED -> GeneratedAuthoring(state, dispatch)
            ClueAuthoringMode.MANUAL -> ManualAuthoring(state, dispatch)
        }
    }

    @Composable
    private fun BoxScope.AuthoringChoice(dispatch: Dispatch) {
        Column(
            modifier = Modifier.align(Alignment.Center).background(
                Color.Gray.copy(alpha = 0.5f),
                RoundedCornerShape(Dimensions.Border.medium)
            ).padding(Dimensions.content),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.Padding.small),
        ) {
            Text(
                text = stringResource(S.clue_authoring_prompt),
                textAlign = TextAlign.Center,
            )
            ElevatedButton(onClick = { dispatch(GenerateClues) }) {
                Text(stringResource(S.generate_clues))
            }
            ElevatedButton(onClick = { dispatch(UseManualAuthoring) }) {
                Text(stringResource(S.write_clue_manually))
            }
        }
    }

    @Composable
    private fun BoxScope.GeneratedAuthoring(state: ScanEditUiState, dispatch: Dispatch) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.Padding.small),
        ) {
            ElevatedButton(onClick = { dispatch(UseManualAuthoring) }) {
                Text(stringResource(S.write_clue_manually))
            }
            ScannedClues(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(Dimensions.content),
                clues = state.clues,
                dispatch = dispatch,
            )
        }
    }

    @Composable
    private fun BoxScope.ManualAuthoring(state: ScanEditUiState, dispatch: Dispatch) {
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().background(
                Color.Gray.copy(alpha = 0.5f),
                RoundedCornerShape(Dimensions.Border.medium)
            ).padding(Dimensions.content),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.Padding.small),
        ) {
            if (state.generationUnavailable) {
                Text(
                    text = stringResource(S.generated_clues_unavailable),
                    textAlign = TextAlign.Center,
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.manualClue,
                onValueChange = { dispatch(UpdateManualClue(it)) },
                label = { Text(stringResource(S.manual_clue)) },
                singleLine = false,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.manualAnswer,
                onValueChange = { dispatch(UpdateManualAnswer(it)) },
                label = { Text(stringResource(S.manual_answer)) },
                singleLine = true,
            )
            if (state.canUseGenerated) {
                ElevatedButton(onClick = { dispatch(UseGeneratedAuthoring) }) {
                    Text(stringResource(S.use_generated_clues))
                }
            }
        }
    }
}
