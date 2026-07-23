package com.micrantha.eyespie.model

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CancellationException

internal sealed interface RuntimeSmokeCheckResult {
    data object Passed : RuntimeSmokeCheckResult

    data class Failed(
        val recoverable: Boolean,
        val diagnosticCode: String,
    ) : RuntimeSmokeCheckResult
}

internal fun interface ModelRuntimeSmokeChecker {
    suspend fun check(model: ReadyModel): RuntimeSmokeCheckResult
}

internal class MediaPipeLlmRuntimeSmokeChecker(
    context: Context,
) : ModelRuntimeSmokeChecker {
    private val applicationContext = context.applicationContext

    override suspend fun check(model: ReadyModel): RuntimeSmokeCheckResult = try {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(model.localPath)
            .setMaxTokens(SMOKE_CHECK_MAX_TOKENS)
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build()
        LlmInference.createFromOptions(applicationContext, options).use { }
        RuntimeSmokeCheckResult.Passed
    } catch (error: CancellationException) {
        throw error
    } catch (error: IllegalArgumentException) {
        RuntimeSmokeCheckResult.Failed(
            recoverable = false,
            diagnosticCode = "runtime.invalid_model",
        )
    } catch (error: UnsatisfiedLinkError) {
        RuntimeSmokeCheckResult.Failed(
            recoverable = false,
            diagnosticCode = "runtime.unavailable",
        )
    } catch (error: Throwable) {
        RuntimeSmokeCheckResult.Failed(
            recoverable = true,
            diagnosticCode = "runtime.load_failed",
        )
    }

    private companion object {
        const val SMOKE_CHECK_MAX_TOKENS = 8
    }
}
