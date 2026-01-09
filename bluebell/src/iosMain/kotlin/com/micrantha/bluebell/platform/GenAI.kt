package com.micrantha.bluebell.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class GenAI {
    actual fun initialize(config: GenAIConfig): Result<Unit> =
        Result.failure(UnsupportedOperationException("GenAI not available on iOS"))

    actual fun newSession(config: GenAIConfig.Session): Result<Unit> =
        Result.failure(UnsupportedOperationException("GenAI not available on iOS"))

    actual fun generate(request: GenAIRequest): Result<String> =
        Result.failure(UnsupportedOperationException("GenAI not available on iOS"))

    actual fun generateFlow(request: GenAIRequest): Flow<String> = emptyFlow()

    actual fun close() = Unit

    actual fun cancel() = Unit
}
