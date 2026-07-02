package com.micrantha.bluebell.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeGenAI : GenAI {
    var generateResult: Result<String> = Result.failure(Exception("Not set"))
    
    override fun initialize(config: GenAIConfig) = Result.success(Unit)
    override fun newSession(config: GenAIConfig.Session) = Result.success(Unit)
    override fun generate(request: GenAIRequest) = generateResult
    override fun generateFlow(request: GenAIRequest): Flow<String> = emptyFlow()
    override fun close() = Unit
    override fun cancel() = Unit
}
