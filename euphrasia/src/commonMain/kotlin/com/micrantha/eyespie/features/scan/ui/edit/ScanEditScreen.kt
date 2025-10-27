package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Init
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Retry
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.entities.ScanEditParams
import com.micrantha.eyespie.features.scan.entities.ScanEditUiState
import eyespie.euphrasia.generated.resources.done
import eyespie.euphrasia.generated.resources.loading_error
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
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.align(Alignment.Center).fillMaxSize(),
                    painter = it,
                    contentDescription = null,
                )
            }

            Box(
                modifier = Modifier.align(Alignment.Center).fillMaxSize().padding(Dimensions.screen)
            ) {
                if (state.isBusy) {

                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).background(
                            Color.Gray.copy(alpha = 0.5f),
                            RoundedCornerShape(Dimensions.Border.medium)
                        ).padding(Dimensions.content).size(Dimensions.progress)
                    )
                } else if (state.isError) {
                    Column(
                        Modifier.align(Alignment.Center).background(
                            Color.Gray.copy(alpha = 0.5f),
                            RoundedCornerShape(Dimensions.Border.medium)
                        ).padding(Dimensions.content),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.heightIn(Dimensions.Padding.small))
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
                    }
                } else {
                    ScannedClues(
                        modifier = Modifier.align(Alignment.Center)
                            .padding(Dimensions.content)
                            .fillMaxWidth(),
                        clues = state.clues,
                        dispatch = dispatch
                    )
                }
            }

            ElevatedButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                enabled = state.enabled,
                onClick = { dispatch(SaveScanEdit) }) {
                Text(stringResource(S.done))
            }
            Spacer(Modifier.height(Dimensions.screen))
        }
    }
}
