package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.source.LLMLocalSource
import com.micrantha.eyespie.core.data.ai.source.ModelSource
import com.micrantha.eyespie.domain.entities.ModelInfo
import com.micrantha.eyespie.domain.repository.AiRepository

class AiDataRepository(
    private val llmLocalSource: LLMLocalSource,
    private val modelSource: ModelSource,
) : AiRepository {

    override var currentModel = modelSource.modelInfo.firstOrNull()
        private set

    override val models: List<ModelInfo>
        get() = modelSource.modelInfo

    override fun isReady(): Boolean {
        return modelSource.modelInfo.all {
            modelSource.exists(it)
        }
    }

    override fun selectModel(model: ModelInfo) {
        currentModel = model
    }

    override suspend fun initialize(): Result<Boolean> {
        if (isReady().not()) {
            return Result.failure(Throwable("LLM not ready"))
        }
        if (currentModel == null) {
            return Result.failure(Throwable("No current model"))
        }
        return llmLocalSource.init(currentModel!!)
    }

}
