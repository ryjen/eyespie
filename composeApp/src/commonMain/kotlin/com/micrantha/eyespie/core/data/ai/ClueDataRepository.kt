package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.mapping.ClueDataMapper
import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.core.data.ai.source.LLMLocalSource
import com.micrantha.eyespie.domain.entities.ColorProof
import com.micrantha.eyespie.domain.entities.DetectProof
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.LabelProof
import com.micrantha.eyespie.domain.repository.ClueRepository
import okio.Path

class ClueDataRepository(
    private val llmLocalSource: LLMLocalSource,
    private val cluePromptSource: CluePromptSource,
    private val clueDataMapper: ClueDataMapper
) : ClueRepository {

    override suspend fun colors(image: Path): Result<ColorProof> {
        return llmLocalSource.generate(cluePromptSource.colorsPrompt, image.toString())
            .map(clueDataMapper::toColorProof)
    }

    override suspend fun detect(image: Path): Result<DetectProof> {
        return llmLocalSource.generate(cluePromptSource.detectPrompt, image.toString())
            .map(clueDataMapper::toDetectProof)
    }

    override suspend fun embedding(image: Path): Result<Embedding> {
        return Result.success(Embedding.EMPTY)
    }

    override suspend fun labels(image: Path): Result<LabelProof> {
        return llmLocalSource.generate(cluePromptSource.labelPrompt, image.toString())
            .map(clueDataMapper::toLabelProof)
    }
}
