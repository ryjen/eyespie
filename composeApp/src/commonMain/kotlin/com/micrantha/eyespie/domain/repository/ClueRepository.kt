package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Clues
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.platform.scan.CameraImage

interface ClueRepository {
    suspend fun generate(image: CameraImage): Result<Clues>
    suspend fun embedding(image: CameraImage): Result<Embedding>
}
