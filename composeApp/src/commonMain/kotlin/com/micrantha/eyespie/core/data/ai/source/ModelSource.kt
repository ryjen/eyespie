package com.micrantha.eyespie.core.data.ai.source

import com.micrantha.bluebell.domain.security.sha256
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.domain.entities.ModelInfo
import com.micrantha.eyespie.domain.entities.UrlFile

fun UrlFile.filename() = sha256(location)

fun Platform.modelPath(file: UrlFile) = modelsPath().resolve(file.filename())

fun Platform.exists(file: UrlFile) = fileExists(modelPath(file))

class ModelSource(private val platform: Platform) {
    // TODO: get from supabase or app configuration
    val modelInfo = listOf(
        ModelInfo(
            name = "SmolVLM2",
            model = UrlFile(
                location = "https://huggingface.co/Cactus-Compute/SmolVLM2-500m-Instruct-GGUF/resolve/main/SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
                checksum = "6f67b8036b2469fcd71728702720c6b51aebd759b78137a8120733b4d66438bc"
            ),
            encoder = UrlFile(
                location = "https://huggingface.co/Cactus-Compute/SmolVLM2-500m-Instruct-GGUF/resolve/main/mmproj-SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
                checksum = "921dc7e259f308e5b027111fa185efcbf33db13f6e35749ddf7f5cdb60ef520b"
            )
        )
    )

    fun exists(info: ModelInfo) =
        platform.exists(info.model) && platform.exists(info.encoder)
}
