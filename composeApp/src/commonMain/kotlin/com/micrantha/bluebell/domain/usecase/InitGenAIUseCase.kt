package com.micrantha.bluebell.domain.usecase

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig

class InitGenAIUseCase(
    private val llm: GenAI
) {
    operator fun invoke(): Result<Unit> = try {
        llm.initialize(
            GenAIConfig(
                modelPath = "gemma3-1b-it-int4.litertlm",
                maxTopK = null,
                maxNumImages = null,
                maxTokens = null,
                visionAdapterPath = "classification_efficientnet_lite.tflite",
                visionEncoderPath = null
            )
        ).getOrThrow()
        llm.newSession(
            GenAIConfig.Session(
                topK = 40,
                topP = 0.95f,
                temperature = 0.8f,
                randomSeed = 0,
                loraPath = "",
                enableVisionModality = true
            )
        )
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
