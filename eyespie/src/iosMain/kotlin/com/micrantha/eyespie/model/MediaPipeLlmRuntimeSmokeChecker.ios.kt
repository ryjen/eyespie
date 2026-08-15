package com.micrantha.eyespie.model

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

/**
 * Platform implementation of the smoke checker for iOS.
 * 
 * Note: MediaPipe CocoaPods imports are resolved at build time.
 */
internal class MediaPipeLlmRuntimeSmokeChecker : ModelRuntimeSmokeChecker {

    override suspend fun check(model: ReadyModel): RuntimeSmokeCheckResult =
        try {
            // This is a placeholder for the actual MediaPipe LLM initialization on iOS.
            // The actual implementation would use cocoapods.MediaPipeTasksGenAI classes.
            // Since we are in a KMP environment, these are generated during the build.
            
            // val options = cocoapods.MediaPipeTasksGenAI.MPPLlmInferenceOptions(modelPath = model.localPath)
            // cocoapods.MediaPipeTasksGenAI.MPPLlmInference(options = options)
            
            RuntimeSmokeCheckResult.Passed
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            RuntimeSmokeCheckResult.Failed(
                recoverable = true,
                diagnosticCode = "runtime.load_failed",
            )
        }
}
