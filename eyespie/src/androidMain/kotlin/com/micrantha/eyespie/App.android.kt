package com.micrantha.eyespie

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.assetpacks.AssetPackManager
import com.micrantha.eyespie.app.EyesPieApp
import com.micrantha.eyespie.model.ModelAssetRepository
import com.micrantha.eyespie.model.ModelAssetState
import com.micrantha.eyespie.model.QueueReason
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

@Composable
fun UIShow() {
    val context = LocalContext.current
    val dependencies = remember(context.applicationContext) {
        androidDependencies(context.applicationContext)
    }

    ModelAssetConfirmationEffect(dependencies)
    EyesPieApp(dependencies)
}

@Composable
private fun ModelAssetConfirmationEffect(dependencies: DI) {
    val repository = remember(dependencies) {
        dependencies.direct.instance<ModelAssetRepository>()
    }
    val assetPackManager = remember(dependencies) {
        dependencies.direct.instance<AssetPackManager>()
    }
    var confirmationShowing by rememberSaveable { mutableStateOf(false) }
    val confirmationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        confirmationShowing = false
    }

    LaunchedEffect(repository, assetPackManager, confirmationLauncher) {
        repository.observe().collect { state ->
            val confirmationRequired = requiresModelAssetConfirmation(state)

            if (confirmationRequired && !confirmationShowing) {
                confirmationShowing = assetPackManager.showConfirmationDialog(confirmationLauncher)
            } else if (!confirmationRequired) {
                confirmationShowing = false
            }
        }
    }
}

internal fun requiresModelAssetConfirmation(state: ModelAssetState): Boolean =
    state is ModelAssetState.Queued && when (state.reason) {
        QueueReason.WaitingForWifi,
        QueueReason.WaitingForPlatformConfirmation,
        -> true

        else -> false
    }
