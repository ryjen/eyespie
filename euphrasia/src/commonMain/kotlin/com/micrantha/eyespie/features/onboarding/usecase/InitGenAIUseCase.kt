package com.micrantha.eyespie.features.onboarding.usecase

import com.micrantha.bluebell.domain.usecase.useCase
import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig

class InitGenAIUseCase(
    private val llm: GenAI
) {
    operator fun invoke(fileName: String): Result<Unit> = useCase {
        llm.initialize(
            GenAIConfig(
                modelPath = fileName,
                maxTopK = null,
                maxNumImages = null,
                maxTokens = null,
                visionAdapterPath = null,
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
    }
}
