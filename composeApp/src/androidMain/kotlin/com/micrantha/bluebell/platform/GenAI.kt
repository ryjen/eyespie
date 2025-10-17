package com.micrantha.bluebell.platform

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.VisionModelOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

actual class GenAI(
    private val context: Context
) {
    private var llm: LlmInference? = null
    private var session: LlmInferenceSession? = null

    actual fun initialize(config: GenAIConfig): Result<Unit> = try {
        if (config.modelPath.isBlank()) throw InvalidModelPathException()

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(copyAssetToFile(context, config.modelPath).absolutePath).apply {
                this.setVisionModelOptions(
                    VisionModelOptions.builder().apply {
                        config.visionEncoderPath?.let {
                            setEncoderPath(it)
                        }
                        config.visionAdapterPath?.let {
                            setAdapterPath(it)
                        }
                    }.build()
                )
                config.maxNumImages?.let {
                    setMaxNumImages(it)
                }
                config.maxTokens?.let {
                    setMaxTokens(it)
                }
                config.maxTopK?.let {
                    setMaxTopK(it)
                }
            }.setPreferredBackend(
                LlmInference.Backend.GPU
            )
            .build()
        this.llm = LlmInference.createFromOptions(context, options)
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    actual fun newSession(config: GenAIConfig.Session): Result<Unit> = try {
        this.llm ?: throw NotInitializedException()

        val options = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(config.topK)
            .setTopP(config.topP)
            .setRandomSeed(config.randomSeed)
            .setLoraPath(config.loraPath)
            .setTemperature(config.temperature)
            .setGraphOptions(
                GraphOptions.builder()
                    .setEnableVisionModality(config.enableVisionModality)
                    .build()
            )
            .build()
        this.session?.close()
        this.session = LlmInferenceSession.createFromOptions(this.llm, options)
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    actual fun generate(request: GenAIRequest): Result<String> = try {
        if (request.prompt.isBlank()) throw InvalidPromptException()
        val response = if (this.session == null) {
            val inference = this.llm ?: throw NotInitializedException()
            inference.generateResponse(request.prompt)
        } else {
            this.session.updateWithRequest(request).generateResponse()
        }
        Result.success(response)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    actual fun generateFlow(request: GenAIRequest): Flow<String> = callbackFlow {
        if (request.prompt.isBlank()) throw InvalidPromptException()

        val listener = { partialResult: String?, done: Boolean ->
            if (partialResult != null) {
                trySend(partialResult)
            }
            if (done) {
                close()
            }
        }

        val response = if (session != null) {
            session.updateWithRequest(request).generateResponseAsync(listener)
        } else {
            val inference = llm ?: throw NotInitializedException()
            inference.generateResponseAsync(request.prompt, listener)
        }

        response.await()

        awaitClose {
            response.cancel(true)
        }
    }

    actual fun close() {
        this.session?.close()
        this.session = null
    }

    actual fun cancel() {
        this.session?.cancelGenerateResponseAsync()
    }

    /**
     * Loads and preprocesses an image for the LLM session.
     * - Resizes to targetWidth x targetHeight (default 512x512)
     * - Converts to MPImage for inference
     */
    private fun preprocessImage(
        uri: String,
        targetWidth: Int = 512,
        targetHeight: Int = 512
    ): MPImage? {
        val file = uri.toUri().toFile()

        if (!file.exists()) return null

        // Decode bitmap
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: return null

        // Resize bitmap
        val resized = bitmap.scale(targetWidth, targetHeight)

        // Convert to MPImage
        return BitmapImageBuilder(resized).build()
    }

    private fun preprocessImages(
        uris: List<String>,
        targetWidth: Int = 512,
        targetHeight: Int = 512
    ) = uris.mapNotNull {
        preprocessImage(it, targetWidth, targetHeight)
    }

    fun LlmInferenceSession?.updateWithRequest(request: GenAIRequest): LlmInferenceSession {
        val inference = this ?: throw SessionRequiredException()
        inference.addQueryChunk(request.prompt)
        preprocessImages(request.images).forEach {
            inference.addImage(it)
        }
        return this
    }

    suspend fun <T> ListenableFuture<T>.await() = suspendCancellableCoroutine { cont ->
        this.addListener({
            try {
                cont.resume(this.get())
            } catch (e: Exception) {
                cont.resume(e)
            }
        }, { runnable -> runnable.run() }) // Direct executor
    }

    fun copyAssetToFile(context: Context, assetName: String): File {
        val file = File(context.filesDir, assetName)
        if (!file.exists()) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file
    }


    inner class NotInitializedException : Exception()
    inner class SessionRequiredException : Exception()
    inner class InvalidModelPathException : Exception()
    inner class InvalidPromptException : Exception()
}
