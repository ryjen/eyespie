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

actual class PlatformGenAI(
    private val context: Context
) : GenAI {
    private var llm: LlmInference? = null
    private var session: LlmInferenceSession? = null
    private var sessionConfig: GenAIConfig.Session? = null

    override fun initialize(config: GenAIConfig): Result<Unit> = try {
        if (config.modelPath.isBlank()) throw InvalidModelPathException()

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(config.modelPath).apply {
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

    override fun newSession(config: GenAIConfig.Session): Result<Unit> = try {
        this.llm ?: throw NotInitializedException()
        sessionConfig = config
        replaceSession(config)
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun generate(request: GenAIRequest): Result<String> = try {
        if (request.prompt.isBlank()) throw InvalidPromptException()
        val response = if (sessionConfig == null) {
            val inference = this.llm ?: throw NotInitializedException()
            inference.generateResponse(request.prompt)
        } else {
            // LlmInferenceSession retains query/image history. Each public generate call is an
            // independent Eyespie inference operation, so start from a fresh configured session.
            freshSession().updateWithRequest(request).generateResponse()
        }
        Result.success(response)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun generateFlow(request: GenAIRequest): Flow<String> = callbackFlow {
        if (request.prompt.isBlank()) throw InvalidPromptException()

        val listener = { partialResult: String?, done: Boolean ->
            if (partialResult != null) {
                trySend(partialResult)
            }
            if (done) {
                close()
            }
        }

        val response = if (sessionConfig != null) {
            freshSession().updateWithRequest(request).generateResponseAsync(listener)
        } else {
            val inference = llm ?: throw NotInitializedException()
            inference.generateResponseAsync(request.prompt, listener)
        }

        response.await()

        awaitClose {
            response.cancel(true)
        }
    }

    override fun close() {
        this.session?.close()
        this.session = null
        this.sessionConfig = null
    }

    override fun cancel() {
        this.session?.cancelGenerateResponseAsync()
    }

    private fun freshSession(): LlmInferenceSession {
        val config = sessionConfig ?: throw SessionRequiredException()
        replaceSession(config)
        return session ?: throw SessionRequiredException()
    }

    private fun replaceSession(config: GenAIConfig.Session) {
        val inference = this.llm ?: throw NotInitializedException()
        this.session?.close()
        this.session = LlmInferenceSession.createFromOptions(
            inference,
            LlmInferenceSession.LlmInferenceSessionOptions.builder()
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
        )
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

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: return null

        val resized = bitmap.scale(targetWidth, targetHeight)

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
        return inference
    }

    suspend fun <T> ListenableFuture<T>.await() = suspendCancellableCoroutine { cont ->
        this.addListener({
            try {
                cont.resume(this.get())
            } catch (e: Exception) {
                cont.resume(e)
            }
        }, { runnable -> runnable.run() })
    }

    fun Context.copyAssetToFile(assetName: String): File {
        val file = File(filesDir, assetName)
        if (!file.exists()) {
            assets.open(assetName).use { input ->
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
