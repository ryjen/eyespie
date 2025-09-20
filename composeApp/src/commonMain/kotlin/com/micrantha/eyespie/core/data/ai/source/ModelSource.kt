package com.micrantha.eyespie.core.data.ai.source

import com.cactus.CactusLM
import com.micrantha.bluebell.domain.security.sha256
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.domain.entities.ModelFile
import com.micrantha.eyespie.domain.entities.ModelInfo
import com.micrantha.eyespie.domain.entities.UrlFile

fun UrlFile.filename() = sha256(location)

fun Platform.modelPath(file: UrlFile) = modelsPath().resolve(file.filename())

class ModelSource(private val platform: Platform, private val llm: CactusLM) {
    suspend fun list() = llm.getModels()

    suspend fun downloadModel(file: ModelFile) = try {
        if (!llm.downloadModel(file.slug)) {
            Result.failure(IllegalStateException("Failed to download default model"))
        } else {
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // TODO: get from supabase or app configuration
    val modelInfo = listOf(
        ModelInfo(
            name = "Qwen3",
            model = UrlFile(
                location = "https://huggingface.co/Cactus-Compute/Qwen3-600m-Instruct-GGUF/resolve/main/Qwen3-0.6B-Q8_0.gguf",
                checksum = "84c0dbe606526d5907251d88ea88b41457f46ce456e9a333d5d2b6245a95cafe"
            )
        )
    )
}
