package com.micrantha.eyespie.game

import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.clue.ClueValidationError
import com.micrantha.eyespie.clue.PlayableClue
import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.MatchEngine
import com.micrantha.eyespie.core.MatchResult
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.core.Thing
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.core.ThingProgressRepository
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.ImageEmbeddingGenerator
import com.micrantha.eyespie.imaging.canonicalImageEmbedding
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex

interface LocalGameIdGenerator {
    fun nextGameId(): GameId
    fun nextThingId(): ThingId
}

enum class LocalGameFailureCode {
    OPERATION_IN_PROGRESS,
    INVALID_GAME_NAME,
    INVALID_CLUE,
    IDENTITY_UNAVAILABLE,
    TARGET_EMBEDDING_FAILED,
    GUESS_EMBEDDING_FAILED,
    GAME_NOT_FOUND,
    THING_NOT_FOUND,
    NOT_LOCAL_CREATOR,
    MATCH_POLICY_INVALID,
    PERSISTENCE_FAILED,
}

data class LocalGameFailure(
    val code: LocalGameFailureCode,
    val clueValidationError: ClueValidationError? = null,
)

sealed interface LocalGameResult<out T> {
    data class Success<T>(val value: T) : LocalGameResult<T>
    data class Failure(val failure: LocalGameFailure) : LocalGameResult<Nothing>
}

data class LocalGameSnapshot(
    val identity: PlayerIdentity,
    val games: List<LocalGameSummary>,
)

data class LocalGameSummary(
    val id: GameId,
    val name: String,
    val things: List<PlayableThingSummary>,
    val localCreator: Boolean = false,
)

data class PlayableThingSummary(
    val id: ThingId,
    val clue: PlayableClue,
    val progress: ThingProgress?,
)

data class CreatedGame(
    val gameId: GameId,
    val thingId: ThingId,
    val name: String,
    val clue: PlayableClue,
)

data class CreatedClue(
    val gameId: GameId,
    val thingId: ThingId,
    val clue: PlayableClue,
)

data class GuessOutcome(
    val gameId: GameId,
    val thingId: ThingId,
    val clue: PlayableClue,
    val match: MatchResult,
    val progress: ThingProgress,
)

class LocalGameLoop(
    private val identityRepository: PlayerIdentityRepository,
    private val gameRepository: GameRepository,
    private val progressRepository: ThingProgressRepository,
    private val embeddingGenerator: ImageEmbeddingGenerator,
    private val idGenerator: LocalGameIdGenerator,
) {
    private val operationMutex = Mutex()

    suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> {
        val identity = try {
            identityRepository.current()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return failure(LocalGameFailureCode.IDENTITY_UNAVAILABLE)
        }

        val games = try {
            gameRepository.list()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return failure(LocalGameFailureCode.PERSISTENCE_FAILED)
        }

        val summaries = try {
            games.map { game ->
                val progressByThing = progressRepository
                    .list(game.id, identity.id)
                    .associateBy(ThingProgress::thingId)
                LocalGameSummary(
                    id = game.id,
                    name = game.name,
                    things = game.things.map { thing ->
                        PlayableThingSummary(
                            id = thing.id,
                            clue = thing.playableClue(),
                            progress = progressByThing[thing.id],
                        )
                    },
                    localCreator = game.creator == identity.id,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return failure(LocalGameFailureCode.PERSISTENCE_FAILED)
        }

        return LocalGameResult.Success(
            LocalGameSnapshot(
                identity = identity,
                games = summaries,
            ),
        )
    }

    suspend fun createGame(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> = exclusiveOperation {
        val normalizedName = normalizeText(name)
        if (normalizedName.isBlank() || normalizedName.length > MAX_GAME_NAME_LENGTH) {
            return@exclusiveOperation failure(LocalGameFailureCode.INVALID_GAME_NAME)
        }

        val clueAuthority = when (val authored = ClueAuthority.manual(clueText, expectedAnswer)) {
            is ClueAuthoringResult.Accepted -> authored.authority
            is ClueAuthoringResult.Rejected -> {
                return@exclusiveOperation LocalGameResult.Failure(
                    LocalGameFailure(
                        code = LocalGameFailureCode.INVALID_CLUE,
                        clueValidationError = authored.error,
                    ),
                )
            }
        }

        val identity = try {
            identityRepository.current()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.IDENTITY_UNAVAILABLE)
        }

        val targetEmbedding = try {
            canonicalImageEmbedding(embeddingGenerator.generate(targetImage))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.TARGET_EMBEDDING_FAILED)
        }

        val gameId = idGenerator.nextGameId()
        val thingId = idGenerator.nextThingId()
        val thing = Thing(
            id = thingId,
            clueAuthority = clueAuthority,
            targetEmbedding = targetEmbedding,
        )
        val game = Game(
            id = gameId,
            name = normalizedName,
            creator = identity.id,
            things = listOf(thing),
        )

        try {
            gameRepository.save(game)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
        }

        LocalGameResult.Success(
            CreatedGame(
                gameId = gameId,
                thingId = thingId,
                name = normalizedName,
                clue = clueAuthority.playable(),
            ),
        )
    }

    suspend fun addClue(
        gameId: GameId,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedClue> = exclusiveOperation {
        val clueAuthority = when (val authored = ClueAuthority.manual(clueText, expectedAnswer)) {
            is ClueAuthoringResult.Accepted -> authored.authority
            is ClueAuthoringResult.Rejected -> {
                return@exclusiveOperation LocalGameResult.Failure(
                    LocalGameFailure(
                        code = LocalGameFailureCode.INVALID_CLUE,
                        clueValidationError = authored.error,
                    ),
                )
            }
        }

        val identity = try {
            identityRepository.current()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.IDENTITY_UNAVAILABLE)
        }

        val game = try {
            gameRepository.get(gameId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
        } ?: return@exclusiveOperation failure(LocalGameFailureCode.GAME_NOT_FOUND)

        if (game.creator != identity.id) {
            return@exclusiveOperation failure(LocalGameFailureCode.NOT_LOCAL_CREATOR)
        }

        val targetEmbedding = try {
            canonicalImageEmbedding(embeddingGenerator.generate(targetImage))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.TARGET_EMBEDDING_FAILED)
        }

        val thingId = idGenerator.nextThingId()
        val thing = Thing(
            id = thingId,
            clueAuthority = clueAuthority,
            targetEmbedding = targetEmbedding,
        )

        try {
            gameRepository.save(game.copy(things = game.things + thing))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
        }

        LocalGameResult.Success(
            CreatedClue(
                gameId = gameId,
                thingId = thingId,
                clue = clueAuthority.playable(),
            ),
        )
    }

    suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> = exclusiveOperation {
        val identity = try {
            identityRepository.current()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.IDENTITY_UNAVAILABLE)
        }

        val game = try {
            gameRepository.get(gameId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
        } ?: return@exclusiveOperation failure(LocalGameFailureCode.GAME_NOT_FOUND)

        val thing = game.things.firstOrNull { it.id == thingId }
            ?: return@exclusiveOperation failure(LocalGameFailureCode.THING_NOT_FOUND)

        val guessEmbedding = try {
            canonicalImageEmbedding(embeddingGenerator.generate(guessImage))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.GUESS_EMBEDDING_FAILED)
        }

        val match = try {
            MatchEngine(thing.matchThreshold).compare(thing.targetEmbedding, guessEmbedding)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.MATCH_POLICY_INVALID)
        }

        val existing = try {
            progressRepository.get(gameId, thingId, identity.id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
        }
        val bestSimilarity = existing?.bestSimilarity?.let { maxOf(it, match.similarity) }
            ?: match.similarity
        val progress = ThingProgress(
            gameId = gameId,
            thingId = thingId,
            playerId = identity.id,
            matched = existing?.matched == true || match.matched,
            bestSimilarity = bestSimilarity,
        )

        try {
            progressRepository.save(progress)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
        }

        LocalGameResult.Success(
            GuessOutcome(
                gameId = gameId,
                thingId = thingId,
                clue = thing.playableClue(),
                match = match,
                progress = progress,
            ),
        )
    }

    private suspend fun <T> exclusiveOperation(
        operation: suspend () -> LocalGameResult<T>,
    ): LocalGameResult<T> {
        if (!operationMutex.tryLock()) {
            return failure(LocalGameFailureCode.OPERATION_IN_PROGRESS)
        }
        return try {
            operation()
        } finally {
            operationMutex.unlock()
        }
    }

    private fun <T> failure(code: LocalGameFailureCode): LocalGameResult<T> =
        LocalGameResult.Failure(LocalGameFailure(code))

    private companion object {
        const val MAX_GAME_NAME_LENGTH = 80

        fun normalizeText(value: String): String = value.trim().replace(Regex("\\s+"), " ")
    }
}
