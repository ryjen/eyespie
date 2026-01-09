package com.micrantha.eyespie.domain.usecase

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig

class InitGenAIUseCase(
    private val llm: GenAI,
    private val onboardingRepository: OnboardingRepository,
    private val loadModelConfig: LoadModelConfig,
    private val platform: Platform
) {
    suspend operator fun invoke(): Result<Unit> {
        if (onboardingRepository.hasGenAI().not()) {
            return Result.success(Unit)
        }

        val modelName = onboardingRepository.genAiModel()
        if (modelName.isNullOrBlank()) {
            return Result.failure(IllegalStateException("has gen ai but no model name"))
        }

        val config = loadModelConfig().getOrThrow()
        val model = config[modelName] ?: throw IllegalStateException("no ai model found for $modelName")

        val filePath = platform.sharedFilesPath().resolve("${model.fileName()}.litertlm")

        model.checksum?.let { checksum ->
            val expected = model.fileName()
            if (!checksum.trim().equals(expected, ignoreCase = true)) {
                return Result.failure(IllegalStateException("invalid checksum"))
            }
        }

        // TODO: all this is remote config
        llm.initialize(
            GenAIConfig(
                modelPath = filePath.toString(),
                maxTopK = null,
                maxNumImages = 3,
                maxTokens = 1024,
                visionAdapterPath = null,
                visionEncoderPath = null
            )
        ).getOrThrow()

        return llm.newSession(
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
