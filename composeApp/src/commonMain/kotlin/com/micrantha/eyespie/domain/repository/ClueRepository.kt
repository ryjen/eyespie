package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.ColorProof
import com.micrantha.eyespie.domain.entities.DetectProof
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.LabelProof
import okio.Path

interface ClueRepository {
    suspend fun labels(image: Path): Result<LabelProof>
    suspend fun colors(image: Path): Result<ColorProof>
    suspend fun detect(image: Path): Result<DetectProof>
    suspend fun embedding(image: Path): Result<Embedding>
}
