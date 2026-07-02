package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.AiProof
import com.micrantha.eyespie.domain.entities.GuessClue
import okio.Path

interface ClueRepository {
    suspend fun clues(image: Path): Result<AiProof>
    suspend fun guess(image: Path, clue: GuessClue): Result<String>
}
