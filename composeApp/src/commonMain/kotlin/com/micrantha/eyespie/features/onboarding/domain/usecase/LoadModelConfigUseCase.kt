package com.micrantha.eyespie.features.onboarding.domain.usecase

import com.micrantha.bluebell.BluebellAssetConfig
import com.micrantha.bluebell.domain.usecase.useCase
import com.micrantha.eyespie.config.EnvConfig
import com.micrantha.eyespie.domain.entities.UrlFile

class LoadModelConfigUseCase {
    operator fun invoke(): Result<Map<String, UrlFile>> = useCase {
        BluebellAssetConfig.load(EnvConfig.ASSET_MANIFEST)?.downloads?.filter {
            it.value.url != null
        }?.mapValues {
            UrlFile(
                it.value.url!!,
                it.value.checksum!!,
            )
        } ?: emptyMap()
    }
}
