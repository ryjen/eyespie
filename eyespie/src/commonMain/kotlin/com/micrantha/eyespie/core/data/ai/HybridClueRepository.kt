package com.micrantha.eyespie.core.data.ai

import com.micrantha.bluebell.observability.logger
import com.micrantha.eyespie.domain.entities.AiProof
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.repository.ClueRepository
import okio.Path

class HybridClueRepository(
    private val genAiRepository: ClueRepository,
    private val platformRepository: ClueRepository
) : ClueRepository {

    private val log by logger()

    override suspend fun clues(image: Path): Result<AiProof> =
        genAiRepository.clues(image).onFailure {
            log.error(it) { "genAiRepository clues failed, falling back to platform" }
        }.recoverCatching {
            platformRepository.clues(image).onFailure {
                log.error(it) { "platformRepository clues failed" }
            }.getOrThrow()
        }

    override suspend fun guess(image: Path, clue: GuessClue): Result<String> =
        genAiRepository.guess(image, clue).onFailure {
            log.error(it) { "genAiRepository guess failed, falling back to platform" }
        }.recoverCatching {
            platformRepository.guess(image, clue).onFailure {
                log.error(it) { "platformRepository guess failed" }
            }.getOrThrow()
        }
}
