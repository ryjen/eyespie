package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.ai.GeneratedClues
import com.micrantha.eyespie.domain.entities.GuessClue
import okio.Path

interface ClueRepository {
    suspend fun clues(image: Path): Result<GeneratedClues>
    suspend fun guess(image: Path, clue: GuessClue): Result<String>
}
