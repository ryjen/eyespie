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
import com.micrantha.eyespie.imaging.ImageRotator
import com.micrantha.eyespie.imaging.MATCH_ROTATIONS
import com.micrantha.eyespie.imaging.ThumbnailCodec
import com.micrantha.eyespie.imaging.canonicalImageEmbedding
import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticOutcome
import com.micrantha.eyespie.telemetry.OperationalTelemetry
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
    NOT_LOCAL_CREATOR,
    TARGET_EMBEDDING_FAILED,
    GUESS_EMBEDDING_FAILED,
    GAME_NOT_FOUND,
    THING_NOT_FOUND,
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

data class AuthoredThing(
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
    private val thumbnailCodec: ThumbnailCodec? = null,
    private val imageRotator: ImageRotator? = null,
    private val telemetry: OperationalTelemetry = OperationalTelemetry(),
) : GameSnapshotLoader {
    private val operationMutex = Mutex()

    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> =
        observeGameOperation(DiagnosticOperation.GAME_SNAPSHOT_LOAD) {
            val identity = try {
                identityRepository.current()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@observeGameOperation failure(LocalGameFailureCode.IDENTITY_UNAVAILABLE)
            }

            val games = try {
                gameRepository.list()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@observeGameOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
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
                return@observeGameOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
            }

            LocalGameResult.Success(LocalGameSnapshot(identity = identity, games = summaries))
        }

    suspend fun createGame(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> = observeGameOperation(DiagnosticOperation.GAME_CREATE) {
        exclusiveOperation {
            val normalizedName = normalizeText(name)
            if (normalizedName.isBlank() || normalizedName.length > MAX_GAME_NAME_LENGTH) {
                return@exclusiveOperation failure(LocalGameFailureCode.INVALID_GAME_NAME)
            }

            val clueAuthority = when (val authored = ClueAuthority.manual(clueText, expectedAnswer)) {
                is ClueAuthoringResult.Accepted -> authored.authority
                is ClueAuthoringResult.Rejected -> return@exclusiveOperation LocalGameResult.Failure(
                    LocalGameFailure(LocalGameFailureCode.INVALID_CLUE, authored.error),
                )
            }

            val identity = try {
                identityRepository.current()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@exclusiveOperation failure(LocalGameFailureCode.IDENTITY_UNAVAILABLE)
            }

            val targetEmbedding = try {
                generateEmbedding(DiagnosticOperation.TARGET_EMBEDDING_GENERATE, targetImage)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@exclusiveOperation failure(LocalGameFailureCode.TARGET_EMBEDDING_FAILED)
            }

            val thumbnail = thumbnailCodec?.encode(targetImage)
            val gameId = idGenerator.nextGameId()
            val thingId = idGenerator.nextThingId()
            val thing = Thing(
                id = thingId,
                clueAuthority = clueAuthority,
                targetEmbedding = targetEmbedding,
                targetThumbnail = thumbnail,
            )
            val game = Game(gameId, normalizedName, identity.id, listOf(thing))

            try {
                telemetry.observe(DiagnosticOperation.GAME_PERSIST) {
                    gameRepository.save(game)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
            }

            LocalGameResult.Success(CreatedGame(gameId, thingId, normalizedName, clueAuthority.playable()))
        }
    }

    suspend fun addClue(
        gameId: GameId,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<AuthoredThing> = observeGameOperation(DiagnosticOperation.CLUE_ADD) {
        exclusiveOperation {
            val clueAuthority = when (val authored = ClueAuthority.manual(clueText, expectedAnswer)) {
                is ClueAuthoringResult.Accepted -> authored.authority
                is ClueAuthoringResult.Rejected -> return@exclusiveOperation LocalGameResult.Failure(
                    LocalGameFailure(LocalGameFailureCode.INVALID_CLUE, authored.error),
                )
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

            if (game.creator != identity.id) return@exclusiveOperation failure(LocalGameFailureCode.NOT_LOCAL_CREATOR)

            val targetEmbedding = try {
                generateEmbedding(DiagnosticOperation.TARGET_EMBEDDING_GENERATE, targetImage)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@exclusiveOperation failure(LocalGameFailureCode.TARGET_EMBEDDING_FAILED)
            }

            val thumbnail = thumbnailCodec?.encode(targetImage)
            val thingId = idGenerator.nextThingId()
            val thing = Thing(
                id = thingId,
                clueAuthority = clueAuthority,
                targetEmbedding = targetEmbedding,
                targetThumbnail = thumbnail,
            )
            try {
                telemetry.observe(DiagnosticOperation.GAME_PERSIST) {
                    gameRepository.save(game.copy(things = game.things + thing))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
            }

            LocalGameResult.Success(AuthoredThing(gameId, thingId, clueAuthority.playable()))
        }
    }

    suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> = observeGameOperation(DiagnosticOperation.GAME_GUESS) {
        exclusiveOperation {
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

            val match = when (
                val rotationMatch = bestRotationMatch(
                    thing.targetEmbedding,
                    guessImage,
                    thing.matchThreshold,
                )
            ) {
                is RotationMatchResult.Success -> rotationMatch.match
                RotationMatchResult.EmbeddingFailed ->
                    return@exclusiveOperation failure(LocalGameFailureCode.GUESS_EMBEDDING_FAILED)
                RotationMatchResult.MatchPolicyInvalid ->
                    return@exclusiveOperation failure(LocalGameFailureCode.MATCH_POLICY_INVALID)
            }

            val existing = try {
                progressRepository.get(gameId, thingId, identity.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
            }
            val bestSimilarity = existing?.bestSimilarity?.let { maxOf(it, match.similarity) } ?: match.similarity
            val progress = ThingProgress(
                gameId = gameId,
                thingId = thingId,
                playerId = identity.id,
                matched = existing?.matched == true || match.matched,
                bestSimilarity = bestSimilarity,
            )

            try {
                telemetry.observe(DiagnosticOperation.PROGRESS_PERSIST) {
                    progressRepository.save(progress)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@exclusiveOperation failure(LocalGameFailureCode.PERSISTENCE_FAILED)
            }

            LocalGameResult.Success(GuessOutcome(gameId, thingId, thing.playableClue(), match, progress))
        }
    }

    private suspend fun <T> observeGameOperation(
        operation: DiagnosticOperation,
        block: suspend () -> LocalGameResult<T>,
    ): LocalGameResult<T> = telemetry.observe(
        operation = operation,
        classify = { result -> result.toDiagnosticOutcome() },
        block = block,
    )

    private suspend fun <T> exclusiveOperation(operation: suspend () -> LocalGameResult<T>): LocalGameResult<T> {
        if (!operationMutex.tryLock()) return failure(LocalGameFailureCode.OPERATION_IN_PROGRESS)
        return try { operation() } finally { operationMutex.unlock() }
    }

    private suspend fun generateEmbedding(
        operation: DiagnosticOperation,
        image: CapturedImage,
    ): List<Float> = telemetry.observe(operation) {
        canonicalImageEmbedding(embeddingGenerator.generate(image))
    }

    /**
     * Compares the stored target embedding against the guess image embedded at
     * each canonical rotation. A player may photograph the target at any angle,
     * and a cosine-similarity embedding is not rotation invariant, so the best
     * rotation's similarity is reported. When no [ImageRotator] is available the
     * guess is embedded once at its captured orientation.
     */
    private suspend fun bestRotationMatch(
        targetEmbedding: List<Float>,
        guessImage: CapturedImage,
        threshold: Double,
    ): RotationMatchResult {
        val rotations = if (imageRotator != null) MATCH_ROTATIONS else listOf(0)
        var best: MatchResult? = null
        var embeddedAtLeastOnce = false

        for (degrees in rotations) {
            val rotated = if (degrees == 0) guessImage else imageRotator?.rotate(guessImage, degrees)
                ?: continue
            val embedding = try {
                generateEmbedding(DiagnosticOperation.GUESS_EMBEDDING_GENERATE, rotated)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                continue
            }
            embeddedAtLeastOnce = true

            val result = try {
                telemetry.observe(DiagnosticOperation.MATCH_EVALUATE) {
                    MatchEngine(threshold).compare(targetEmbedding, embedding)
                }
            } catch (_: Exception) {
                return RotationMatchResult.MatchPolicyInvalid
            }
            best = if (best == null || result.similarity > best!!.similarity) result else best
        }

        return when {
            best != null -> RotationMatchResult.Success(best!!)
            !embeddedAtLeastOnce -> RotationMatchResult.EmbeddingFailed
            else -> RotationMatchResult.MatchPolicyInvalid
        }
    }

    private fun <T> failure(code: LocalGameFailureCode): LocalGameResult<T> =
        LocalGameResult.Failure(LocalGameFailure(code))

    private fun LocalGameResult<*>.toDiagnosticOutcome(): DiagnosticOutcome = when (this) {
        is LocalGameResult.Success -> DiagnosticOutcome.Success
        is LocalGameResult.Failure -> DiagnosticOutcome.failed(failure.code.toDiagnosticCode())
    }

    private fun LocalGameFailureCode.toDiagnosticCode(): DiagnosticCode = when (this) {
        LocalGameFailureCode.OPERATION_IN_PROGRESS -> DiagnosticCode.GAME_OPERATION_IN_PROGRESS
        LocalGameFailureCode.INVALID_GAME_NAME -> DiagnosticCode.GAME_INVALID_NAME
        LocalGameFailureCode.INVALID_CLUE -> DiagnosticCode.GAME_INVALID_CLUE
        LocalGameFailureCode.IDENTITY_UNAVAILABLE -> DiagnosticCode.IDENTITY_UNAVAILABLE
        LocalGameFailureCode.NOT_LOCAL_CREATOR -> DiagnosticCode.NOT_LOCAL_CREATOR
        LocalGameFailureCode.TARGET_EMBEDDING_FAILED -> DiagnosticCode.TARGET_EMBEDDING_FAILED
        LocalGameFailureCode.GUESS_EMBEDDING_FAILED -> DiagnosticCode.GUESS_EMBEDDING_FAILED
        LocalGameFailureCode.GAME_NOT_FOUND -> DiagnosticCode.GAME_NOT_FOUND
        LocalGameFailureCode.THING_NOT_FOUND -> DiagnosticCode.THING_NOT_FOUND
        LocalGameFailureCode.MATCH_POLICY_INVALID -> DiagnosticCode.MATCH_POLICY_INVALID
        LocalGameFailureCode.PERSISTENCE_FAILED -> DiagnosticCode.PERSISTENCE_FAILED
    }

    private sealed interface RotationMatchResult {
        data class Success(val match: MatchResult) : RotationMatchResult
        data object EmbeddingFailed : RotationMatchResult
        data object MatchPolicyInvalid : RotationMatchResult
    }

    private companion object {
        const val MAX_GAME_NAME_LENGTH = 80
        fun normalizeText(value: String): String = value.trim().replace(Regex("\\s+"), " ")
    }
}
