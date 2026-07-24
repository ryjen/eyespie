package com.micrantha.eyespie.model

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManager
import okio.FileSystem
import org.kodein.di.DI
import org.kodein.di.bindInstance
import org.kodein.di.bindSingleton

internal val androidSmokeModelDescriptor = ModelAssetDescriptor(
    id = "eyespie-offline-model",
    version = "unstaged",
    filename = "eyespie-offline-model-unstaged.task",
    expectedBytes = 0L,
    sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
    runtime = ModelRuntimeCompatibility(
        engine = "mediapipe",
        minimumRuntimeVersion = "0.10.35",
        minimumModelAbi = 1,
    ),
)

internal val androidModelRuntimeCapabilities = ModelRuntimeCapabilities(
    engine = "mediapipe",
    version = "0.10.35",
    modelAbi = 1,
)

internal fun androidModelAssetModule(
    context: Context,
    assetPackManager: AssetPackManager,
    descriptor: ModelAssetDescriptor = androidSmokeModelDescriptor,
    verifier: ModelAssetVerifier = ModelAssetVerifier(FileSystem.SYSTEM),
    runtime: ModelRuntimeCapabilities = androidModelRuntimeCapabilities,
    smokeChecker: ModelRuntimeSmokeChecker = MediaPipeLlmRuntimeSmokeChecker(context),
) = DI.Module("AndroidModelAsset") {
    bindInstance<AssetPackManager> { assetPackManager }
    bindSingleton<ModelAssetRepository> {
        PlayAssetDeliveryModelRepository(
            assetPackManager = assetPackManager,
            descriptor = descriptor,
            verifier = verifier,
            runtime = runtime,
            smokeChecker = smokeChecker,
        )
    }
}
