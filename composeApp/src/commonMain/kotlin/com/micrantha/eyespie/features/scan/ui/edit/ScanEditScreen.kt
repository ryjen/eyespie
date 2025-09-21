package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.app.Scaffolding
import com.micrantha.bluebell.app.Scaffolding.CanGoBack
import com.micrantha.bluebell.app.navi.NavAction
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.components.StateRenderer
import com.micrantha.bluebell.ui.screen.ScaffoldScreen
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.core.ui.component.ChoiceField
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ClearColor
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ClearDetection
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ClearLabel
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ColorChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.CustomColorChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.CustomDetectionChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.CustomLabelChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.DetectionChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.Init
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.LabelChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.NameChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.SaveScanEdit
import eyespie.composeapp.generated.resources.category
import eyespie.composeapp.generated.resources.color
import eyespie.composeapp.generated.resources.new_thing
import eyespie.composeapp.generated.resources.things
import org.jetbrains.compose.resources.stringResource

class ScanEditScreen(
    context: ScreenContext,
    private val params: ScanEditParams
) : ScaffoldScreen(context), StateRenderer<ScanEditUiState> {

    @Composable
    override fun Render() {
        val screenModel: ScanEditScreenModel = rememberScreenModel()

        val title = stringResource(S.new_thing)

        LaunchedEffect(title) {
            screenModel.dispatch(Init(params))
            screenModel.dispatch(Scaffolding.Title(title))
            screenModel.dispatch(CanGoBack(true))
            screenModel.dispatch(
                Scaffolding.Actions(
                    listOf(
                        NavAction(
                            icon = Icons.Default.Save,
                            action = {
                                it.dispatcher.dispatch(SaveScanEdit)
                            }
                        )
                    )
                )
            )
        }

        val state by screenModel.state.collectAsState()

        Render(state, screenModel)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(state: ScanEditUiState, dispatch: Dispatch) {

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier.fillMaxSize()
                .scrollable(state = scrollState, orientation = Orientation.Vertical)
        ) {
            state.image?.let {
                Image(
                    modifier = Modifier.align(Alignment.CenterHorizontally).sequentialFieldPadding(),
                    painter = it,
                    contentDescription = null,
                )
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth().sequentialFieldPadding(),
                value = state.name,
                onValueChange = { dispatch(NameChanged(it)) },
                label = { Text(text = "Name") },
                singleLine = true,
                maxLines = 1,
                placeholder = { Text(text = "Enter an identifying name") }
            )
            ChoiceField(
                modifier = Modifier.fillMaxWidth().sequentialFieldPadding(),
                choices = state.labels,
                label = { Text(stringResource(S.category)) },
                onValue = {
                    state.customLabel ?: it.label
                },
                trailingIcon = {
                    if (state.customLabel != null)
                        IconButton(onClick = { dispatch(ClearLabel) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                        } else null
                },
                onCustom = {
                    dispatch(CustomLabelChanged(it))
                }
            ) { choice ->
                dispatch(LabelChanged(choice))
            }

            ChoiceField(
                modifier = Modifier.fillMaxWidth().sequentialFieldPadding(),
                choices = state.colors,
                label = { Text(stringResource(S.color)) },
                trailingIcon = {
                    if (state.customLabel != null)
                        IconButton(onClick = { dispatch(ClearColor) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                        }
                },
                onValue = {
                    state.customColor ?: it.label
                },
                onCustom = {
                    dispatch(CustomColorChanged(it))
                }
            ) { choice ->
                dispatch(ColorChanged(choice))
            }

            ChoiceField(
                modifier = Modifier.fillMaxWidth().sequentialFieldPadding(),
                choices = state.detections,
                label = { Text(stringResource(S.things)) },
                trailingIcon = {
                    if (state.customDetection != null)
                        IconButton(onClick = { dispatch(ClearDetection) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                        }
                },
                onValue = {
                    state.customDetection ?: it.label
                },
                onCustom = {
                    dispatch(CustomDetectionChanged(it))
                }
            ) { choice ->
                dispatch(DetectionChanged(choice))
            }
        }
    }

    fun Modifier.sequentialFieldPadding() = padding(horizontal = Dimensions.content).padding(top = Dimensions.content)
}
