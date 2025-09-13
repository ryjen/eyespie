package com.micrantha.eyespie.core.data.ai.source

import com.cactus.CactusVLM
import com.micrantha.eyespie.domain.entities.ModelInfo

class LLMLocalSource(
    private val llm: CactusVLM
) {
    suspend fun init(info: ModelInfo) = try {
        Result.success(
            llm.init(
                info.model.filename(),
                info.encoder.filename()
            )
        )
    } catch (e: Throwable) {
        Result.failure(e)
    }

    suspend fun generate(prompt: String, imagePath: String) = try {
        val result = llm.completion(
            prompt = prompt,
            imagePath = imagePath
        )
        if (result == null) {
            throw IllegalStateException("No result")
        }
        Result.success(result)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    suspend fun download() = try {
        Result.success(
            llm.download()
        )
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
