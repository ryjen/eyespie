package com.micrantha.eyespie.model

import org.kodein.di.DI

internal fun iosModelAssetModule(
    capabilities: IosModelDeliveryCapabilities = DefaultIosModelDeliveryCapabilities,
): DI.Module = modelAssetModule(IosModelAssetRepository(capabilities))
