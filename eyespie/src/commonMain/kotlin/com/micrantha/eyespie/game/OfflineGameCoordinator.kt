package com.micrantha.eyespie.game

import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.clue.ClueValidationError
import com.micrantha.eyespie.clue.PlayableClue
import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.MatchEngine
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.core.Thing
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.core.ThingProgressRepository
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.ImageEmbeddingGenerator
import com.micrantha.eyespie.imaging.canonicalImageEmbedding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

private const val MAX_GAME_NAME_LENGTH = 120

/** User-owned inputs required to create the smallest closed-alpha challenge. */
data class ManualGameDraft(
    val name: String,
    val clueText: String,
    val expectedAnswer: String,
)

enum class OfflineFailureCode {
    BUSY,
    INVALID_GAME_NAME,
    INVALID_CLUE,
    IDENTITY_UNAVAILABLE,
    EMBEDDING_FAILED,
    INVALID_EMBEDDING,
    GAME_NOT_FOUND,
    THING_NOT_FOUND,
    PERSISTENCE_FAILED,
    ID_GENERATION_FAILED,
}

data class OfflineFailure(
    val code: OfflineFailureCode,
    val clueValidationError: ClueValidationError? = null,
)

sealed interface OfflineResult<out T> {
    data class Success<T>(val value: T) : OfflineResult<T>
    data class Failure(val failure: OfflineFailure) : OfflineResult<Nothing>
}

/** Explicit allowlisted play state. Creator-only clue authority cannot cross this boundary. */
data class PlayableThingState(
    val id: ThingId,
    val clue: PlayableClue,
    val matched: Boolean,
    val bestSimilarity: Double?,
)

data class PlayableGameState(
    val id: GameId,
    val name: String,
    val things: List<PlayableThingState>,
)

data class GuessOutcome(
    val similarity: Double,
    val matched: Boolean,
    val game: PlayableGameState,
)

enum class OfflineRuntimeUnavailableReason {
    LOCAL_STORAGE_UNAVAILABLE,
}

sealed interface OfflineRuntimeState {
    data class Ready(val coordinator: OfflineGameCoordinator) : OfflineRuntimeState
    data class Unavailable(val reason: OfflineRuntimeUnavailableReason) : OfflineRuntimeState
}

/**
 * Application-owned orchestration for the backendless create/play loop.
 *
 * Capture lifecycle remains platform-owned. This coordinator accepts only copied [CapturedImage]
 * values and never persists them. A non-queueing mutex bounds target/guess embedding work to one
 * owned operation at a time; cancellation is always rethrown rather than converted to failure.
 */
class OfflineGameCoordinator(
    private val identityRepository: PlayerIdentityRepository,
    private val gameRepository: GameRepository,
    private val progressRepository: ThingProgressRepository,
    private val embeddingGenerator: ImageEmbeddingGenerator,
    private val idGenerator: () -> String,
    private val matchThreshold: Double = MatchEngine.DEFAULT_THRESHOLD,
) {
    private val operationMutex = Mutex()

    init {
        require(matchThreshold in -1.0..1.0) { "match threshold must be a cosine similarity" }
    }

    fun validateDraft(draft: ManualGameDraft): OfflineFailure? {
        val gameName = normalize(draft.name)
        if (gameName.isBlank() || gameName.length > MAX_GAME_NAME_LENGTH) {
            return OfflineFailure(OfflineFailureCode.INVALID_GAME_NAME)
        }
        return when (val clue = ClueAuthority.manual(draft.clueText, draft.expectedAnswer)) {
            is ClueAuthoringResult.Accepted -> null
            is ClueAuthoringResult.Rejected -> OfflineFailure(
                code = OfflineFailureCode.INVALID_CLUE,
                clueValidationError = clue.error,
            )
        }
    }

    suspend fun createManualGame(
        draft: ManualGameDraft,
        targetImage: CapturedImage,
    ): OfflineResult<PlayableGameState> {
        val prepared = prepareDraft(draft)
        if (prepared is PreparedDraft.Invalid) return OfflineResult.Failure(prepared.failure)
        prepared as PreparedDraft.Valid

        if (!operationMutex.tryLock()) return busy()
        return try {
            withContext(Dispatchers.Default) {
                val identity = currentIdentity() ?: return@withContext failure(
                    OfflineFailureCode.IDENTITY_UNAVAILABLE,
                )
                val targetEmbedding = when (val embedded = embed(targetImage)) {
                    is OfflineResult.Success -> embedded.value
                    is OfflineResult.Failure -> return@withContext embedded
                }
                val ids = generateIds() ?: return@withContext failure(
                    OfflineFailureCode.ID_GENERATION_FAILED,
                )
                val game = Game(
                    id = ids.first,
                    name = prepared.name,
                    creator = identity.id,
                    things = listOf(
                        Thing(
                            id = ids.second,
                            clueAuthority = prepared.clueAuthority,
                            targetEmbedding = targetEmbedding,
                            matchThreshold = matchThreshold,
                        ),
                    ),
                )

                try {
                    if (gameRepository.get(game.id) != null) {
                        return@withContext failure(OfflineFailureCode.ID_GENERATION_FAILED)
                    }
                    gameRepository.save(game)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    return@withContext failure(OfflineFailureCode.PERSISTENCE_FAILED)
                }

                OfflineResult.Success(game.toPlayable(emptyMap()))
            }
        } finally {
            operationMutex.unlock()
        }
    }

    suspend fun loadGames(): OfflineResult<List<PlayableGameState>> = withContext(Dispatchers.Default) {
        val identity = currentIdentity() ?: return@withContext failure(
            OfflineFailureCode.IDENTITY_UNAVAILABLE,
        )
        try {
            val games = gameRepository.list()
            OfflineResult.Success(
                games.map { game ->
                    val progress = progressRepository.list(game.id, identity.id)
                        .associateBy(ThingProgress::thingId)
                    game.toPlayable(progress)
                },
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            failure(OfflineFailureCode.PERSISTENCE_FAILED)
        }
    }

    suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): OfflineResult<GuessOutcome> {
        if (!operationMutex.tryLock()) return busy()
        return try {
            withContext(Dispatchers.Default) {
                val identity = currentIdentity() ?: return@withContext failure(
                    OfflineFailureCode.IDENTITY_UNAVAILABLE,
                )
                val game = try {
                    gameRepository.get(gameId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    return@withContext failure(OfflineFailureCode.PERSISTENCE_FAILED)
                } ?: return@withContext failure(OfflineFailureCode.GAME_NOT_FOUND)
                val thing = game.things.firstOrNull { it.id == thingId }
                    ?: return@withContext failure(OfflineFailureCode.THING_NOT_FOUND)

                val targetEmbedding = canonicalizePersisted(thing.targetEmbedding)
                    ?: return@withContext failure(OfflineFailureCode.INVALID_EMBEDDING)
                val guessEmbedding = when (val embedded = embed(guessImage)) {
                    is OfflineResult.Success -> embedded.value
                    is OfflineResult.Failure -> return@withContext embedded
                }
                val match = try {
                    MatchEngine(thing.matchThreshold).compare(targetEmbedding, guessEmbedding)
                } catch (_: IllegalArgumentException) {
                    return@withContext failure(OfflineFailureCode.INVALID_EMBEDDING)
                }
                if (!match.similarity.isFinite()) {
                    return@withContext failure(OfflineFailureCode.INVALID_EMBEDDING)
                }
                val similarity = match.similarity.coerceIn(-1.0, 1.0)

                val progressByThing = try {
                    progressRepository.list(game.id, identity.id).associateBy(ThingProgress::thingId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    return@withContext failure(OfflineFailureCode.PERSISTENCE_FAILED)
                }.toMutableMap()
                val previous = progressByThing[thing.id]
                val progress = ThingProgress(
                    gameId = game.id,
                    thingId = thing.id,
                    playerId = identity.id,
                    matched = previous?.matched == true || match.matched,
                    bestSimilarity = previous?.bestSimilarity?.let { maxOf(it, similarity) } ?: similarity,
                )
                try {
                    progressRepository.save(progress)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    return@withContext failure(OfflineFailureCode.PERSISTENCE_FAILED)
                }
                progressByThing[thing.id] = progress

                OfflineResult.Success(
                    GuessOutcome(
                        similarity = similarity,
                        matched = progress.matched,
                        game = game.toPlayable(progressByThing),
                    ),
                )
            }
        } finally {
            operationMutex.unlock()
        }
    }

    private suspend fun currentIdentity(): PlayerIdentity? = try {
        identityRepository.current()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        null
    }

    private suspend fun embed(image: CapturedImage): OfflineResult<List<Float>> {
        val generated = try {
            embeddingGenerator.generate(image)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            return failure(OfflineFailureCode.EMBEDDING_FAILED)
        }
        val canonical = canonicalizePersisted(generated)
            ?: return failure(OfflineFailureCode.INVALID_EMBEDDING)
        return OfflineResult.Success(canonical)
    }

    private fun canonicalizePersisted(values: List<Float>): List<Float>? = try {
        canonicalImageEmbedding(values)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun generateIds(): Pair<GameId, ThingId>? = try {
        val gameToken = idGenerator().trim()
        val thingToken = idGenerator().trim()
        if (gameToken.isBlank() || thingToken.isBlank()) return null
        GameId("game:$gameToken") to ThingId("thing:$thingToken")
    } catch (_: Throwable) {
        null
    }

    private fun prepareDraft(draft: ManualGameDraft): PreparedDraft {
        val normalizedName = normalize(draft.name)
        if (normalizedName.isBlank() || normalizedName.length > MAX_GAME_NAME_LENGTH) {
            return PreparedDraft.Invalid(OfflineFailure(OfflineFailureCode.INVALID_GAME_NAME))
        }
        return when (val clue = ClueAuthority.manual(draft.clueText, draft.expectedAnswer)) {
            is ClueAuthoringResult.Accepted -> PreparedDraft.Valid(normalizedName, clue.authority)
            is ClueAuthoringResult.Rejected -> PreparedDraft.Invalid(
                OfflineFailure(
                    code = OfflineFailureCode.INVALID_CLUE,
                    clueValidationError = clue.error,
                ),
            )
        }
    }

    private fun Game.toPlayable(progress: Map<ThingId, ThingProgress>): PlayableGameState =
        PlayableGameState(
            id = id,
            name = name,
            things = things.map { thing ->
                val thingProgress = progress[thing.id]
                PlayableThingState(
                    id = thing.id,
                    clue = thing.playableClue(),
                    matched = thingProgress?.matched == true,
                    bestSimilarity = thingProgress?.bestSimilarity,
                )
            },
        )

    private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")

    private fun busy(): OfflineResult.Failure = failure(OfflineFailureCode.BUSY)

    private fun failure(code: OfflineFailureCode): OfflineResult.Failure =
        OfflineResult.Failure(OfflineFailure(code))

    private sealed interface PreparedDraft {
        data class Valid(val name: String, val clueAuthority: ClueAuthority) : PreparedDraft
        data class Invalid(val failure: OfflineFailure) : PreparedDraft
    }
}
