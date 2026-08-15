package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.ai.GeneratedClues
import com.micrantha.eyespie.domain.entities.GuessClue
import okio.Path

interface ClueRepository {
    val canGenerateClues: Boolean
        get() = false

    suspend fun clues(image: Path): Result<GeneratedClues>
    suspend fun guess(image: Path, clue: GuessClue): Result<String>
}
