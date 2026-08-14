package com.micrantha.eyespie.domain.usecase

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailabilityController
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapabilities
import com.micrantha.eyespie.domain.ai.SemanticInferenceDiagnosticCode
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceReasonCode
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityException
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityVerifier

class InitGenAIUseCase(
    private val llm: GenAI,
    private val onboardingRepository: OnboardingRepository,
    private val loadModelConfig: LoadModelConfig,
    private val platform: Platform,
    private val modelIntegrityVerifier: ModelIntegrityVerifier,
    private val inferenceProvider: SemanticInferenceProvider,
    private val availabilityController: SemanticInferenceAvailabilityController,
) {
    suspend operator fun invoke(): Result<Unit> {
        val platformState = inferenceProvider.availability.value
        if (
            platformState is SemanticInferenceAvailability.Unavailable &&
            platformState.reasonCode == SemanticInferenceReasonCode.PLATFORM_IMAGE_INPUT_UNSUPPORTED
        ) {
            return Result.success(Unit)
        }

        if (onboardingRepository.hasGenAI().not()) {
            availabilityController.markNotConfigured()
            return Result.success(Unit)
        }

        availabilityController.markInitializing()

        return try {
            val modelName = onboardingRepository.genAiModel()
            if (modelName.isNullOrBlank()) {
                availabilityController.markUnavailable(SemanticInferenceReasonCode.MODEL_NOT_CONFIGURED)
                return Result.failure(IllegalStateException("has gen ai but no model name"))
            }

            val config = loadModelConfig().getOrThrow()
            val model = config[modelName]
            if (model == null) {
                availabilityController.markUnavailable(SemanticInferenceReasonCode.MODEL_UNAVAILABLE)
                return Result.failure(IllegalStateException("configured ai model is unavailable"))
            }

            val filePath = platform.sharedFilesPath().resolve("${model.fileName()}.litertlm")
            modelIntegrityVerifier.verify(model, filePath).getOrThrow()

            // TODO: move generation parameters to application-owned remote configuration.
            llm.initialize(
                GenAIConfig(
                    modelPath = filePath.toString(),
                    maxTopK = null,
                    maxNumImages = 3,
                    maxTokens = MAX_CONTEXT_TOKENS,
                    visionAdapterPath = null,
                    visionEncoderPath = null,
                )
            ).getOrThrow()

            // Validate the configured runtime session once. Android PlatformGenAI retains only the
            // configuration and constructs a fresh native session for every logical request.
            llm.newSession(SESSION_CONFIG).getOrThrow()

            availabilityController.markAvailable(
                capabilities = CAPABILITIES,
                identity = inferenceProvider.identity.copy(modelId = modelName),
            )
            Result.success(Unit)
        } catch (error: ModelIntegrityException) {
            availabilityController.markFailed(SemanticInferenceDiagnosticCode.MODEL_INTEGRITY_FAILED)
            Result.failure(error)
        } catch (error: UnsupportedOperationException) {
            availabilityController.markUnavailable(
                SemanticInferenceReasonCode.PLATFORM_IMAGE_INPUT_UNSUPPORTED,
            )
            Result.failure(error)
        } catch (error: Throwable) {
            availabilityController.markFailed(
                SemanticInferenceDiagnosticCode.RUNTIME_INITIALIZATION_FAILED,
            )
            Result.failure(error)
        }
    }

    private companion object {
        const val MAX_CONTEXT_TOKENS = 1024

        val SESSION_CONFIG = GenAIConfig.Session(
            topK = 40,
            topP = 0.95f,
            temperature = 0.8f,
            randomSeed = 0,
            loraPath = "",
            enableVisionModality = true,
        )

        val CAPABILITIES = SemanticInferenceCapabilities(
            textGeneration = true,
            imageInput = true,
            streaming = true,
            cancellation = true,
            maxContextTokens = MAX_CONTEXT_TOKENS,
        )
    }
}
