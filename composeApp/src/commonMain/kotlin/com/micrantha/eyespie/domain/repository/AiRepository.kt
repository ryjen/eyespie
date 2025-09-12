package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.ModelInfo

interface AiRepository {
    suspend fun initialize(): Result<Boolean>

    val currentModel: ModelInfo?

    val models: List<ModelInfo>

    fun selectModel(model: ModelInfo)

    fun isReady(): Boolean
}
