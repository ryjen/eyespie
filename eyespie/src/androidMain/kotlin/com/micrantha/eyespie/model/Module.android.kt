package com.micrantha.eyespie.model

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManager
import okio.FileSystem
import org.kodein.di.DI
import org.kodein.di.bindInstance
import org.kodein.di.bindSingleton

internal val androidSmokeModelDescriptor = ModelAssetDescriptor(
    id = "eyespie-offline-model-smoke-test",
    version = "pad-smoke-2026-07-20.1",
    filename = "pad-smoke-test.bin",
    expectedBytes = 39L,
    sha256 = "a9be80b9c833cb32e1a41ae404bdebf1b5d74ab2e85d8ca6ad44e5fd7e824ddf",
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
