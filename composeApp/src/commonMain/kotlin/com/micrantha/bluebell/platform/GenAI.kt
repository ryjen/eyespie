package com.micrantha.bluebell.platform

import kotlinx.coroutines.flow.Flow

data class GenAIConfig(
    val modelPath: String, // Required: .task model path on device
    val maxTopK: Int?, // default: 64 (for session initialization)
    val maxNumImages: Int?, // default: 1
    val maxTokens: Int?, // default: 512
    val visionEncoderPath: String?, // Optional: vision encoder model path for multimodal
    val visionAdapterPath: String? // Optional: vision adapter model path for multimodal
) {

    data class Session(
        // Optional generation settings
        val topK: Int, // default: 40
        val topP: Float, // default: 0.95
        val temperature: Float, // default: 0.8
        val randomSeed: Int, // default: 0
        val loraPath: String, // LoRA customization (GPU only)
        val enableVisionModality: Boolean
    )
}

data class GenAIRequest(
    // Required
    val prompt: String,
    // Multimodal support
    val images: List<String>
)

expect class GenAI {

    fun initialize(config: GenAIConfig): Result<Unit>

    fun newSession(config: GenAIConfig.Session): Result<Unit>

    fun generate(request: GenAIRequest): Result<String>

    fun generateFlow(request: GenAIRequest): Flow<String>

    fun close()

    fun cancel()
}
