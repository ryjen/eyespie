package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.AiProof
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.entities.GuessProof
import kotlinx.coroutines.flow.Flow
import okio.Path

interface ClueRepository {
    fun clues(image: Path): Result<AiProof>
    fun guess(image: Path, clue: GuessClue): Result<String>
}
