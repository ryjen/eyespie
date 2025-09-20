package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.ModelFile

interface AiRepository {

    suspend fun initialize(model: ModelFile? = null): Result<Unit>

    suspend fun listModels(): Result<List<ModelFile>>

    suspend fun downloadModel(model: ModelFile): Result<Unit>
}
