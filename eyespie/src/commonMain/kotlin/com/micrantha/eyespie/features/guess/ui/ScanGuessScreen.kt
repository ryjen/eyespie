package com.micrantha.eyespie.features.guess.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.features.guess.ui.ScanGuessAction.ImageCaptured
import com.micrantha.eyespie.platform.scan.CameraScanner
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.camera.CAMERA
import com.micrantha.eyespie.generated.resources.scan_cold
import com.micrantha.eyespie.generated.resources.scan_hot
import com.micrantha.eyespie.generated.resources.scan_ice_cold
import com.micrantha.eyespie.generated.resources.scan_warm
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.compose.rememberInstance

class ScanGuessScreen(
    private val args: ScanGuessArgs
) : Screen {

    constructor(id: String) : this(ScanGuessArgs(id))

    @Composable
    override fun Content() {
        val permissions by rememberInstance<PermissionsController>()

        val screenModel: ScanGuessScreenModel = rememberScreenModel(arg = args)

        LaunchedEffect(Unit) {
            permissions.providePermission(Permission.CAMERA)
        }

        val state by screenModel.state.collectAsState()

        Render(state, screenModel)
    }

    @Composable
    private fun Render(state: ScanGuessUiState, dispatch: Dispatch) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CameraScanner(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxSize(),
            ) {
                dispatch.send(ImageCaptured(it))
            }

            state.similarity?.let { similarity ->
                val message = when {
                    similarity > 0.8f -> stringResource(S.scan_hot)
                    similarity > 0.6f -> stringResource(S.scan_warm)
                    similarity > 0.4f -> stringResource(S.scan_cold)
                    else -> stringResource(S.scan_ice_cold)
                }
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                )
            }
        }
    }

    @Composable
    private fun BoxScope.Matched() {
        Column(
            modifier = Modifier.align(Alignment.Center)
        ) {}
    }
}
