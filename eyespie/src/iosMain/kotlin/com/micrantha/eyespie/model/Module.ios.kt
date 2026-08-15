package com.micrantha.eyespie.model

import okio.FileSystem
import org.kodein.di.DI
import org.kodein.di.bindSingleton

internal val iosSmokeModelDescriptor = ModelAssetDescriptor(
    id = "eyespie-offline-model",
    version = "gemma-3n-E2B-it-int4-local",
    filename = "gemma-3n-E2B-it-int4.litertlm",
    expectedBytes = 3655827456L,
    sha256 = "2ed7bc3a0026c93d5b8a4544b352d9d00cd66ff0bac3ef6a20ac3d2cba4010d6",
    runtime = ModelRuntimeCompatibility(
        engine = "mediapipe",
        minimumRuntimeVersion = "0.10.35",
        minimumModelAbi = 1,
    ),
)

internal val iosModelRuntimeCapabilities = ModelRuntimeCapabilities(
    engine = "mediapipe",
    version = "0.10.35",
    modelAbi = 1,
)

internal fun iosModelAssetModule(
    capabilities: IosModelDeliveryCapabilities = DefaultIosModelDeliveryCapabilities,
    transport: IosModelAssetTransport = UnconfiguredIosModelAssetTransport,
    descriptor: ModelAssetDescriptor = iosSmokeModelDescriptor,
    verifier: ModelAssetVerifier = ModelAssetVerifier(FileSystem.SYSTEM),
    runtime: ModelRuntimeCapabilities = iosModelRuntimeCapabilities,
    smokeChecker: ModelRuntimeSmokeChecker = MediaPipeLlmRuntimeSmokeChecker(),
): DI.Module = DI.Module("IosModelAsset") {
    bindSingleton<ModelAssetRepository> {
        IosModelAssetRepository(
            capabilities = capabilities,
            transport = transport,
            descriptor = descriptor,
            verifier = verifier,
            runtime = runtime,
            smokeChecker = smokeChecker,
        )
    }
}
