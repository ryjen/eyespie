package com.micrantha.bluebell.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class PlatformGenAI : GenAI {
    override fun initialize(config: GenAIConfig): Result<Unit> =
        Result.failure(UnsupportedOperationException("GenAI not available on iOS"))

    override fun newSession(config: GenAIConfig.Session): Result<Unit> =
        Result.failure(UnsupportedOperationException("GenAI not available on iOS"))

    override fun generate(request: GenAIRequest): Result<String> =
        Result.failure(UnsupportedOperationException("GenAI not available on iOS"))

    override fun generateFlow(request: GenAIRequest): Flow<String> = emptyFlow()

    override fun close() = Unit

    override fun cancel() = Unit
}
