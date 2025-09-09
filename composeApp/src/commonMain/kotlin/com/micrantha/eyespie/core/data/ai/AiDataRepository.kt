package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.source.LLMLocalSource
import com.micrantha.eyespie.domain.entities.ModelInfo
import com.micrantha.eyespie.domain.repository.AiRepository

class AiDataRepository(
    private val llmLocalSource: LLMLocalSource,
): AiRepository {

    override fun getCurrentModelInfo(): ModelInfo {
        return llmLocalSource.modelInfo
    }

    override suspend fun initialize(): Result<Boolean> =
        llmLocalSource.init()

}
