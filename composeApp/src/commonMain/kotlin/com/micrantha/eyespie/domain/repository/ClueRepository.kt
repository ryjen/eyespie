package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Clues
import kotlinx.coroutines.flow.Flow
import okio.Path

interface ClueRepository {
    fun generate(image: Path): Result<Clues>
    fun infer(image: Path): Flow<Clues>
}
