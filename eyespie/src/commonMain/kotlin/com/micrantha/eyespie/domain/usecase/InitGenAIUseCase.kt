package com.micrantha.eyespie.domain.usecase

import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailabilityController
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapabilities
import com.micrantha.eyespie.domain.ai.SemanticInferenceDiagnosticCode
import com.micrantha.eyespie.domain.ai.SemanticInferenceInitialization
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceProviderSetup
import com.micrantha.eyespie.domain.ai.SemanticInferenceReasonCode
import com.micrantha.eyespie.domain.ai.SemanticInferenceSamplingConfiguration
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityException
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityVerifier
import kotlinx.coroutines.CancellationException

class InitGenAIUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val loadModelConfig: LoadModelConfig,
    private val platform: Platform,
    private val modelIntegrityVerifier: ModelIntegrityVerifier,
    private val inferenceProvider: SemanticInferenceProvider,
    private val providerSetup: SemanticInferenceProviderSetup,
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

            providerSetup.initialize(
                SemanticInferenceInitialization(
                    modelPath = filePath,
                    identity = inferenceProvider.identity.copy(modelId = modelName),
                    capabilities = CAPABILITIES,
                    maxImages = MAX_IMAGES,
                    sampling = SAMPLING,
                )
            ).getOrThrow()
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: ModelIntegrityException) {
            availabilityController.markFailed(SemanticInferenceDiagnosticCode.MODEL_INTEGRITY_FAILED)
            Result.failure(error)
        } catch (error: Throwable) {
            if (inferenceProvider.availability.value !is SemanticInferenceAvailability.Failed) {
                availabilityController.markFailed(
                    SemanticInferenceDiagnosticCode.RUNTIME_INITIALIZATION_FAILED,
                )
            }
            Result.failure(error)
        }
    }

    private companion object {
        const val MAX_CONTEXT_TOKENS = 1024
        const val MAX_IMAGES = 3

        val SAMPLING = SemanticInferenceSamplingConfiguration(
            topK = 40,
            topP = 0.95f,
            temperature = 0.8f,
            randomSeed = 0,
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
