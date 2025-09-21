package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.source.AgentLocalSource
import com.micrantha.eyespie.core.data.ai.source.ModelSource
import com.micrantha.eyespie.core.data.storage.source.PreferencesLocalSource
import com.micrantha.eyespie.domain.entities.ModelFile
import com.micrantha.eyespie.domain.repository.AiRepository

class AgentDataRepository(
    private val llmLocalSource: AgentLocalSource,
    private val modelSource: ModelSource,
    private val preferencesLocalSource: PreferencesLocalSource
) : AiRepository {

    override suspend fun listModels(): Result<List<ModelFile>> = try {
        Result.success(modelSource.list().map {
            ModelFile(
                downloadUrl = it.download_url,
                name = it.name,
                slug = it.slug
            )
        })
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun downloadModel(model: ModelFile): Result<Unit> {
        return modelSource.downloadModel(model).onSuccess {
            preferencesLocalSource["model"] = model.slug
        }
    }

    override suspend fun initialize(model: ModelFile?): Result<Unit> {
        return llmLocalSource.init(model?.slug).mapCatching { result ->
            if (result.not()) {
                throw Throwable("LLM init failed")
            }
        }
    }
}
