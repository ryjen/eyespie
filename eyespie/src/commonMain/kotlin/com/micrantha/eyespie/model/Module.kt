package com.micrantha.eyespie.model

import org.kodein.di.DI
import org.kodein.di.bindInstance

/**
 * Binds the platform model-delivery implementation behind the shared contract.
 *
 * Android and iOS composition roots provide their repository implementation;
 * tests may provide a test implementation of [ModelAssetRepository].
 */
internal fun modelAssetModule(repository: ModelAssetRepository) = DI.Module("ModelAsset") {
    bindInstance<ModelAssetRepository> { repository }
}
