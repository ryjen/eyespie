package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.components.StateRenderer
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.core.ui.Screen
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadAction.Done
import com.micrantha.eyespie.features.scan.components.ScannedClues
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Init
import com.micrantha.eyespie.features.scan.entities.ScanEditParams
import com.micrantha.eyespie.features.scan.entities.ScanEditUiState
import eyespie.euphrasia.generated.resources.done
import eyespie.euphrasia.generated.resources.new_thing
import org.jetbrains.compose.resources.stringResource

class ScanEditScreen(
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(state: ScanEditUiState, dispatch: Dispatch) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            state.image?.let {
                Image(
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.align(Alignment.Center).fillMaxSize(),
                    painter = it,
                    contentDescription = null,
                )
            }

            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                        .sizeIn(Dimensions.touchable)
                )
            } else {
                ScannedClues(
                    modifier = Modifier.align(Alignment.Center)
                        .padding(Dimensions.screen)
                        .fillMaxWidth(),
                    clues = state.clues,
                    dispatch = dispatch
                )
            }

            OutlinedButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                enabled = state.enabled,
                onClick = { dispatch(Done) }) {
                Text(stringResource(S.done))
            }
        }
    }
}
