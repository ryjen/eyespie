package com.micrantha.eyespie.model

import org.kodein.di.DI

internal fun iosModelAssetModule(
    capabilities: IosModelDeliveryCapabilities = DefaultIosModelDeliveryCapabilities,
    transport: IosModelAssetTransport = UnconfiguredIosModelAssetTransport,
): DI.Module = modelAssetModule(
    IosModelAssetRepository(
        capabilities = capabilities,
        transport = transport,
    ),
)
