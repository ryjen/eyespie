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
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume

actual class PlatformGenAI(
    private val context: Context
) : GenAI {
    private var llm: LlmInference? = null
    private var activeSession: LlmInferenceSession? = null
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

        // Validate that the configured session can be constructed, then discard it. Public
        // inference calls create a fresh session so independent Eyespie operations share no
        // prompt or image history.
        createSession(config).close()
        sessionConfig = config
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
            val operationSession = freshOperationSession()
            try {
                operationSession.updateWithRequest(request).generateResponse()
            } finally {
                closeOperationSession(operationSession)
            }
        }
        Result.success(response)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun generateFlow(request: GenAIRequest): Flow<String> = callbackFlow {
        if (request.prompt.isBlank()) throw InvalidPromptException()

        var operationSession: LlmInferenceSession? = null
        val listener = { partialResult: String?, done: Boolean ->
            if (partialResult != null) {
                trySend(partialResult)
            }
            if (done) {
                operationSession?.let(::closeOperationSession)
                close()
            }
        }

        try {
            if (sessionConfig != null) {
                val newSession = freshOperationSession()
                operationSession = newSession
                newSession.updateWithRequest(request)
            }

            val response = operationSession?.generateResponseAsync(listener)
                ?: (llm ?: throw NotInitializedException()).generateResponseAsync(request.prompt, listener)

            response.awaitResult()
            awaitClose {
                response.cancel(true)
                operationSession?.let(::closeOperationSession)
            }
        } finally {
            operationSession?.let(::closeOperationSession)
        }
    }

    override fun close() {
        closeActiveSession()
        sessionConfig = null
    }

    override fun cancel() {
        activeSession?.cancelGenerateResponseAsync()
    }

    private fun freshOperationSession(): LlmInferenceSession {
        val config = sessionConfig ?: throw SessionRequiredException()
        closeActiveSession()
        return createSession(config).also {
            activeSession = it
        }
    }

    private fun createSession(config: GenAIConfig.Session): LlmInferenceSession {
        val inference = this.llm ?: throw NotInitializedException()
        return LlmInferenceSession.createFromOptions(
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

    private fun closeOperationSession(operationSession: LlmInferenceSession) {
        if (activeSession === operationSession) {
            operationSession.close()
            activeSession = null
        }
    }

    private fun closeActiveSession() {
        activeSession?.close()
        activeSession = null
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

    private suspend fun <T> ListenableFuture<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                cancel(true)
            }
            addListener({
                if (!continuation.isActive) return@addListener
                try {
                    continuation.resume(get())
                } catch (cancelled: java.util.concurrent.CancellationException) {
                    continuation.cancel(cancelled)
                } catch (failed: ExecutionException) {
                    continuation.resumeWith(Result.failure(failed.cause ?: failed))
                } catch (failed: Throwable) {
                    continuation.resumeWith(Result.failure(failed))
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
