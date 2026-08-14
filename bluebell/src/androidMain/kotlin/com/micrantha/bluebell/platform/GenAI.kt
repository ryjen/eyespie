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
    private val sessionLock = Any()
    private var llm: LlmInference? = null
    private var initializing = false
    private var activeSession: LlmInferenceSession? = null
    private var closingSession: LlmInferenceSession? = null
    private var failedSession: LlmInferenceSession? = null
    private var pendingClosingCancellation: Pair<LlmInferenceSession, ListenableFuture<*>>? = null
    private var pendingClosingSessionCancellation: LlmInferenceSession? = null
    private var sessionLifecycleFailure: Throwable? = null
    private var sessionConfig: GenAIConfig.Session? = null

    override fun initialize(config: GenAIConfig): Result<Unit> {
        if (config.modelPath.isBlank()) return Result.failure(InvalidModelPathException())

        return try {
            val failedToRecover = synchronized(sessionLock) {
                if (initializing || activeSession != null || closingSession != null) {
                    throw OperationInProgressException()
                }
                initializing = true
                sessionConfig = null
                failedSession?.also {
                    failedSession = null
                    closingSession = it
                }
            }

            if (failedToRecover != null) {
                try {
                    failedToRecover.cancelGenerateResponseAsync()
                } catch (error: Throwable) {
                    synchronized(sessionLock) {
                        if (closingSession === failedToRecover) closingSession = null
                        failedSession = failedToRecover
                        sessionLifecycleFailure = error
                    }
                    throw error
                }
                closeClaimedSession(failedToRecover)
            }

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
            val newRuntime = LlmInference.createFromOptions(context, options)

            synchronized(sessionLock) {
                llm = newRuntime
                sessionLifecycleFailure = null
                initializing = false
            }
            Result.success(Unit)
        } catch (err: Throwable) {
            synchronized(sessionLock) {
                initializing = false
            }
            Result.failure(err)
        }
    }

    override fun newSession(config: GenAIConfig.Session): Result<Unit> = try {
        // Validate the configured session once, but keep the validation teardown inside the
        // same lifecycle slot as normal request teardown. Public inference calls still create
        // fresh request sessions so independent Eyespie operations share no hidden context.
        val validationSession = synchronized(sessionLock) {
            requireSessionSlotAvailable()
            createSession(config).also { closingSession = it }
        }
        closeClaimedSession(validationSession)
        synchronized(sessionLock) {
            requireSessionRuntimeHealthy()
            sessionConfig = config
        }
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun generate(request: GenAIRequest): Result<String> = try {
        if (request.prompt.isBlank()) throw InvalidPromptException()
        val configured = synchronized(sessionLock) {
            requireSessionRuntimeHealthy()
            sessionConfig != null
        }
        val response = if (!configured) {
            if (request.images.isNotEmpty()) throw SessionRequiredException()
            val inference = this.llm ?: throw NotInitializedException()
            inference.generateResponse(request.prompt)
        } else {
            val operationSession = freshOperationSession()
            var primaryFailure: Throwable? = null
            try {
                operationSession.updateWithRequest(request).generateResponse()
            } catch (error: Throwable) {
                primaryFailure = error
                throw error
            } finally {
                try {
                    closeOperationSession(operationSession)
                } catch (cleanup: Throwable) {
                    primaryFailure?.addSuppressed(cleanup) ?: throw cleanup
                }
            }
        }
        Result.success(response)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun generateFlow(request: GenAIRequest): Flow<String> = callbackFlow {
        if (request.prompt.isBlank()) throw InvalidPromptException()

        var operationSession: LlmInferenceSession? = null
        var primaryFailure: Throwable? = null
        val listener = { partialResult: String?, _: Boolean ->
            if (partialResult != null) {
                trySend(partialResult)
            }
        }

        try {
            val configured = synchronized(sessionLock) {
                requireSessionRuntimeHealthy()
                sessionConfig != null
            }
            if (configured) {
                val newSession = freshOperationSession()
                operationSession = newSession
                newSession.updateWithRequest(request)
            } else if (request.images.isNotEmpty()) {
                throw SessionRequiredException()
            }

            val response = operationSession?.generateResponseAsync(listener)
                ?: (llm ?: throw NotInitializedException()).generateResponseAsync(request.prompt, listener)

            response.awaitResult(operationSession)
            channel.close()
            awaitClose {
                cancelResponse(response, operationSession)
            }
        } catch (error: Throwable) {
            primaryFailure = error
            throw error
        } finally {
            operationSession?.let {
                try {
                    closeOperationSession(it)
                } catch (cleanup: Throwable) {
                    primaryFailure?.addSuppressed(cleanup) ?: throw cleanup
                }
            }
        }
    }

    override fun close() {
        val sessionToClose = synchronized(sessionLock) {
            requireSessionRuntimeHealthy()
            sessionConfig = null
            if (closingSession != null) throw OperationInProgressException()
            activeSession?.also {
                activeSession = null
                closingSession = it
            }
        }
        sessionToClose?.let(::closeClaimedSession)
    }

    override fun cancel() {
        synchronized(sessionLock) {
            when {
                activeSession != null -> activeSession?.cancelGenerateResponseAsync()
                closingSession != null -> pendingClosingSessionCancellation = closingSession
                failedSession != null -> failedSession?.cancelGenerateResponseAsync()
            }
        }
    }

    private fun freshOperationSession(): LlmInferenceSession = synchronized(sessionLock) {
        requireSessionRuntimeHealthy()
        val config = sessionConfig ?: throw SessionRequiredException()
        if (activeSession != null || closingSession != null) throw OperationInProgressException()
        createSession(config).also {
            activeSession = it
        }
    }

    private fun requireSessionSlotAvailable() {
        requireSessionRuntimeHealthy()
        if (activeSession != null || closingSession != null) throw OperationInProgressException()
    }

    private fun requireSessionRuntimeHealthy() {
        if (initializing) throw OperationInProgressException()
        sessionLifecycleFailure?.let { throw SessionLifecycleFailedException(it) }
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
        val ownsSession = synchronized(sessionLock) {
            if (activeSession === operationSession) {
                activeSession = null
                closingSession = operationSession
                true
            } else {
                false
            }
        }
        if (ownsSession) {
            closeClaimedSession(operationSession)
        }
    }

    private fun closeClaimedSession(session: LlmInferenceSession) {
        try {
            session.close()
        } catch (error: Throwable) {
            val pending = synchronized(sessionLock) {
                if (closingSession === session) closingSession = null
                failedSession = session
                sessionLifecycleFailure = error

                val response = pendingClosingCancellation
                    ?.takeIf { it.first === session }
                    ?.second
                val cancelSession = pendingClosingSessionCancellation === session

                if (pendingClosingCancellation?.first === session) {
                    pendingClosingCancellation = null
                }
                if (cancelSession) {
                    pendingClosingSessionCancellation = null
                }
                response to cancelSession
            }

            if (pending.second) {
                try {
                    session.cancelGenerateResponseAsync()
                } catch (cancellationFailure: Throwable) {
                    error.addSuppressed(cancellationFailure)
                }
            }
            try {
                pending.first?.cancel(true)
            } catch (cancellationFailure: Throwable) {
                error.addSuppressed(cancellationFailure)
            }
            throw error
        }
        synchronized(sessionLock) {
            if (closingSession === session) closingSession = null
            if (pendingClosingCancellation?.first === session) {
                pendingClosingCancellation = null
            }
            if (pendingClosingSessionCancellation === session) {
                pendingClosingSessionCancellation = null
            }
        }
    }

    private fun cancelResponse(
        response: ListenableFuture<*>,
        operationSession: LlmInferenceSession?,
    ) {
        synchronized(sessionLock) {
            if (response.isDone) return
            when {
                operationSession == null -> response.cancel(true)
                activeSession === operationSession -> cancelOwnedResponse(operationSession, response)
                closingSession === operationSession -> {
                    pendingClosingSessionCancellation = operationSession
                    pendingClosingCancellation = operationSession to response
                }
                failedSession === operationSession -> cancelOwnedResponse(operationSession, response)
            }
        }
    }

    private fun cancelOwnedResponse(
        session: LlmInferenceSession,
        response: ListenableFuture<*>,
    ) {
        var failure: Throwable? = null
        try {
            session.cancelGenerateResponseAsync()
        } catch (error: Throwable) {
            failure = error
        }
        try {
            response.cancel(true)
        } catch (error: Throwable) {
            failure?.addSuppressed(error) ?: run { failure = error }
        }
        failure?.let { throw it }
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
    ) = uris.map {
        preprocessImage(it, targetWidth, targetHeight)
            ?: throw InvalidImageInputException()
    }

    fun LlmInferenceSession?.updateWithRequest(request: GenAIRequest): LlmInferenceSession {
        val inference = this ?: throw SessionRequiredException()
        inference.addQueryChunk(request.prompt)
        preprocessImages(request.images).forEach {
            inference.addImage(it)
        }
        return inference
    }

    private suspend fun <T> ListenableFuture<T>.awaitResult(
        operationSession: LlmInferenceSession?,
    ): T = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancelResponse(this, operationSession)
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
    inner class OperationInProgressException : Exception()
    inner class SessionLifecycleFailedException(cause: Throwable) : IllegalStateException(cause)
    inner class InvalidModelPathException : Exception()
    inner class InvalidPromptException : Exception()
    inner class InvalidImageInputException : Exception()
}
