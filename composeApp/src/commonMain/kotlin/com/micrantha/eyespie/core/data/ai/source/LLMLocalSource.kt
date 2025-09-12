package com.micrantha.eyespie.core.data.ai.source

import com.cactus.CactusVLM
import com.micrantha.bluebell.domain.security.hash
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.domain.entities.ModelInfo

class LLMLocalSource(
    private val platform: Platform
) {
    private val llm = CactusVLM()

    suspend fun init(model: ModelInfo) = try {
        Result.success(
            llm.init(
                platform.filePath(hash(model.name)).toString()
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
        if (result != null) {
            Result.success(result)
        } else {
            Result.failure(Throwable("No result"))
        }
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
