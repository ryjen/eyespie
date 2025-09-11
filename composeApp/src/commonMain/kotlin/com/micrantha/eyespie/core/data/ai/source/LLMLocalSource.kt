package com.micrantha.eyespie.core.data.ai.source

import com.cactus.CactusVLM
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.domain.entities.ModelInfo

class LLMLocalSource(
    private val platform: Platform
) {
    private val llm = CactusVLM()

    // TODO: get from supabase or app configuration
    val modelInfo = ModelInfo(
        url = "https://huggingface.co/unsloth/gemma-3-270m-it-GGUF/resolve/main/gemma-3-270m-it-Q8_0.gguf?download=true",
        fileName = "gemma-3-270m-it-Q8_0.gguf",
        name = "Gemma3 AI Model"
    )

    fun isReady() = platform.fileExists(platform.filePath(modelInfo.fileName))

    suspend fun init() = try {
        Result.success(
            llm.init(
                platform.filePath(modelInfo.fileName).toString()
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
