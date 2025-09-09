package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.ModelInfo

interface AiRepository {
    suspend fun initialize(): Result<Boolean>

    fun getCurrentModelInfo(): ModelInfo
}
