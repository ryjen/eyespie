package com.micrantha.eyespie.features.onboarding.usecase

import com.micrantha.bluebell.domain.usecase.useCase
import com.micrantha.bluebell.platform.Platform
import com.micrantha.bluebell.plugin.BluebellAssetConfig
import com.micrantha.eyespie.config.EnvConfig
import io.ktor.util.decodeBase64String
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.io.encoding.Base64

class LoadModelConfigUseCase(
    private val platform: Platform
) {
    @OptIn(ExperimentalSerializationApi::class)
    operator fun invoke(): Result<Map<String, BluebellAssetConfig.Download>> = useCase {
        val config: BluebellAssetConfig? =
            platform.asset(EnvConfig.ASSET_MANIFEST.toPath()).let {
            Json.decodeFromString(it.readUtf8().decodeBase64String())
        }
        config?.downloads?.filter {
            config.models.containsKey(it.key)
        } ?: emptyMap()
    }
}
