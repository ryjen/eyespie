package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.core.Thing
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.identity.SigningIdentity
import com.micrantha.eyespie.identity.playerIdFor
import com.micrantha.eyespie.imaging.canonicalImageEmbedding
import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticOutcome
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class GameBundleExportFailureCode {
    GAME_NOT_FOUND,
    IDENTITY_UNAVAILABLE,
    SIGNING_IDENTITY_MISMATCH,
    NOT_LOCAL_CREATOR,
    INVALID_GAME,
    SIGNING_FAILED,
    SIGNATURE_SELF_CHECK_FAILED,
}

sealed interface GameBundleExportResult {
    data class Success(
        val gameId: GameId,
        val bytes: ByteArray,
    ) : GameBundleExportResult

    data class Failure(val code: GameBundleExportFailureCode) : GameBundleExportResult
}

enum class GameBundleImportFailureCode {
    CREATOR_ID_MISMATCH,
    INVALID_SIGNATURE,
    SIGNATURE_VERIFICATION_FAILED,
    INVALID_GAME,
    PERSISTENCE_FAILED,
}

data class GameBundleImportPreview(
    val gameId: GameId,
    val gameName: String,
    val creatorPlayerId: PlayerId,
    val thingCount: Int,
)

sealed interface GameBundleImportPreviewResult {
    data class Ready(val preview: GameBundleImportPreview) : GameBundleImportPreviewResult
    data class InvalidFormat(val code: GameBundleFailureCode) : GameBundleImportPreviewResult
    data class Failure(val code: GameBundleImportFailureCode) : GameBundleImportPreviewResult
}

sealed interface GameBundleImportResult {
    data class Imported(val gameId: GameId) : GameBundleImportResult
    data class AlreadyPresent(val gameId: GameId) : GameBundleImportResult
    data class Conflict(val gameId: GameId) : GameBundleImportResult

    data class InvalidFormat(val code: GameBundleFailureCode) : GameBundleImportResult
    data class Failure(val code: GameBundleImportFailureCode) : GameBundleImportResult
}

/**
 * Application-owned portable game service.
 *
 * Preview verification performs the same format, creator, signature, and domain checks as import,
 * but does not inspect or mutate the local repository. A later import always revalidates the bytes
 * before the import mutex enters the local-authority decision, so preview is never authorization.
 * The mutex serializes the check/save pair within one application process so a conflicting same-ID
 * import cannot race an otherwise valid import into the repository.
 *
 * Operational telemetry observes only the closed operation/result taxonomy. Portable bytes, game
 * identifiers, clue content, embeddings, keys, signatures, and exception prose never enter records.
 */
class GameBundleService(
    private val identityRepository: PlayerIdentityRepository,
    private val signingIdentity: SigningIdentity,
    private val gameRepository: GameRepository,
    private val codec: GameBundleCodec = GameBundleCodec(),
    private val telemetry: OperationalTelemetry = OperationalTelemetry(),
) {
    private val importMutex = Mutex()

    suspend fun export(gameId: GameId): GameBundleExportResult = telemetry.observe(
        operation = DiagnosticOperation.BUNDLE_EXPORT,
        classify = GameBundleExportResult::toDiagnosticOutcome,
    ) {
        exportUnobserved(gameId)
    }

    suspend fun previewImport(bytes: ByteArray): GameBundleImportPreviewResult = telemetry.observe(
        operation = DiagnosticOperation.BUNDLE_IMPORT_PREVIEW,
        classify = GameBundleImportPreviewResult::toDiagnosticOutcome,
    ) {
        previewImportUnobserved(bytes)
    }

    suspend fun import(bytes: ByteArray): GameBundleImportResult = telemetry.observe(
        operation = DiagnosticOperation.BUNDLE_IMPORT,
        classify = GameBundleImportResult::toDiagnosticOutcome,
    ) {
        importUnobserved(bytes)
    }

    private suspend fun exportUnobserved(gameId: GameId): GameBundleExportResult {
        val identity = try {
            identityRepository.current()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.IDENTITY_UNAVAILABLE)
        }

        val publicKey = try {
            signingIdentity.publicKey()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.IDENTITY_UNAVAILABLE)
        }

        val signingPlayerId = try {
            playerIdFor(publicKey)
        } catch (_: Exception) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.SIGNING_IDENTITY_MISMATCH)
        }
        if (signingPlayerId != identity.id) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.SIGNING_IDENTITY_MISMATCH)
        }

        val game = try {
            gameRepository.get(gameId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.GAME_NOT_FOUND)
        } ?: return GameBundleExportResult.Failure(GameBundleExportFailureCode.GAME_NOT_FOUND)

        if (game.creator != identity.id) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.NOT_LOCAL_CREATOR)
        }

        val portable = try {
            game.toPortable(publicKey)
        } catch (_: Exception) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.INVALID_GAME)
        }
        val unsigned = try {
            codec.encodeUnsigned(portable)
        } catch (_: Exception) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.INVALID_GAME)
        }
        val signature = try {
            signingIdentity.sign(unsigned)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.SIGNING_FAILED)
        }

        val selfVerified = try {
            signingIdentity.verify(publicKey, unsigned, signature)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        if (!selfVerified) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.SIGNATURE_SELF_CHECK_FAILED)
        }

        val bytes = try {
            codec.encodeSigned(unsigned, signature)
        } catch (_: Exception) {
            return GameBundleExportResult.Failure(GameBundleExportFailureCode.SIGNING_FAILED)
        }
        return GameBundleExportResult.Success(game.id, bytes)
    }

    private suspend fun previewImportUnobserved(bytes: ByteArray): GameBundleImportPreviewResult {
        val decoded = when (val result = codec.decode(bytes)) {
            is GameBundleDecodeResult.Success -> result.bundle
            is GameBundleDecodeResult.Failure -> return GameBundleImportPreviewResult.InvalidFormat(result.code)
        }

        val creatorId = try {
            playerIdFor(decoded.game.creatorPublicKey)
        } catch (_: Exception) {
            return GameBundleImportPreviewResult.Failure(GameBundleImportFailureCode.CREATOR_ID_MISMATCH)
        }
        if (creatorId.value != decoded.game.creatorPlayerId) {
            return GameBundleImportPreviewResult.Failure(GameBundleImportFailureCode.CREATOR_ID_MISMATCH)
        }

        val verified = try {
            signingIdentity.verify(
                decoded.game.creatorPublicKey,
                decoded.unsignedBytes,
                decoded.signature,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return GameBundleImportPreviewResult.Failure(
                GameBundleImportFailureCode.SIGNATURE_VERIFICATION_FAILED,
            )
        }
        if (!verified) {
            return GameBundleImportPreviewResult.Failure(GameBundleImportFailureCode.INVALID_SIGNATURE)
        }

        val importedGame = try {
            decoded.game.toImportedGame()
        } catch (_: Exception) {
            return GameBundleImportPreviewResult.Failure(GameBundleImportFailureCode.INVALID_GAME)
        }

        return GameBundleImportPreviewResult.Ready(
            GameBundleImportPreview(
                gameId = importedGame.id,
                gameName = importedGame.name,
                creatorPlayerId = importedGame.creator,
                thingCount = importedGame.things.size,
            ),
        )
    }

    private suspend fun importUnobserved(bytes: ByteArray): GameBundleImportResult {
        val decoded = when (val result = codec.decode(bytes)) {
            is GameBundleDecodeResult.Success -> result.bundle
            is GameBundleDecodeResult.Failure -> return GameBundleImportResult.InvalidFormat(result.code)
        }

        val creatorId = try {
            playerIdFor(decoded.game.creatorPublicKey)
        } catch (_: Exception) {
            return GameBundleImportResult.Failure(GameBundleImportFailureCode.CREATOR_ID_MISMATCH)
        }
        if (creatorId.value != decoded.game.creatorPlayerId) {
            return GameBundleImportResult.Failure(GameBundleImportFailureCode.CREATOR_ID_MISMATCH)
        }

        val verified = try {
            signingIdentity.verify(
                decoded.game.creatorPublicKey,
                decoded.unsignedBytes,
                decoded.signature,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return GameBundleImportResult.Failure(
                GameBundleImportFailureCode.SIGNATURE_VERIFICATION_FAILED,
            )
        }
        if (!verified) {
            return GameBundleImportResult.Failure(GameBundleImportFailureCode.INVALID_SIGNATURE)
        }

        val importedGame = try {
            decoded.game.toImportedGame()
        } catch (_: Exception) {
            return GameBundleImportResult.Failure(GameBundleImportFailureCode.INVALID_GAME)
        }

        return importMutex.withLock {
            val existing = try {
                gameRepository.get(importedGame.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@withLock GameBundleImportResult.Failure(
                    GameBundleImportFailureCode.PERSISTENCE_FAILED,
                )
            }

            if (existing != null) {
                return@withLock if (existing.portableContentEquivalent(decoded.game)) {
                    GameBundleImportResult.AlreadyPresent(existing.id)
                } else {
                    GameBundleImportResult.Conflict(existing.id)
                }
            }

            try {
                gameRepository.save(importedGame)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@withLock GameBundleImportResult.Failure(
                    GameBundleImportFailureCode.PERSISTENCE_FAILED,
                )
            }
            GameBundleImportResult.Imported(importedGame.id)
        }
    }
}

private fun GameBundleExportResult.toDiagnosticOutcome(): DiagnosticOutcome = when (this) {
    is GameBundleExportResult.Success -> DiagnosticOutcome.Success
    is GameBundleExportResult.Failure -> DiagnosticOutcome.failed(
        when (code) {
            GameBundleExportFailureCode.GAME_NOT_FOUND -> DiagnosticCode.GAME_NOT_FOUND
            GameBundleExportFailureCode.IDENTITY_UNAVAILABLE -> DiagnosticCode.IDENTITY_UNAVAILABLE
            GameBundleExportFailureCode.SIGNING_IDENTITY_MISMATCH -> DiagnosticCode.SIGNING_IDENTITY_MISMATCH
            GameBundleExportFailureCode.NOT_LOCAL_CREATOR -> DiagnosticCode.NOT_LOCAL_CREATOR
            GameBundleExportFailureCode.INVALID_GAME -> DiagnosticCode.BUNDLE_INVALID_GAME
            GameBundleExportFailureCode.SIGNING_FAILED -> DiagnosticCode.BUNDLE_SIGNING_FAILED
            GameBundleExportFailureCode.SIGNATURE_SELF_CHECK_FAILED ->
                DiagnosticCode.BUNDLE_SIGNATURE_SELF_CHECK_FAILED
        },
    )
}

private fun GameBundleImportPreviewResult.toDiagnosticOutcome(): DiagnosticOutcome = when (this) {
    is GameBundleImportPreviewResult.Ready -> DiagnosticOutcome.Success
    is GameBundleImportPreviewResult.InvalidFormat ->
        DiagnosticOutcome.failed(DiagnosticCode.BUNDLE_INVALID_FORMAT)
    is GameBundleImportPreviewResult.Failure -> DiagnosticOutcome.failed(code.toDiagnosticCode())
}

private fun GameBundleImportResult.toDiagnosticOutcome(): DiagnosticOutcome = when (this) {
    is GameBundleImportResult.Imported,
    is GameBundleImportResult.AlreadyPresent,
    -> DiagnosticOutcome.Success
    is GameBundleImportResult.Conflict -> DiagnosticOutcome.failed(DiagnosticCode.BUNDLE_CONFLICT)
    is GameBundleImportResult.InvalidFormat -> DiagnosticOutcome.failed(DiagnosticCode.BUNDLE_INVALID_FORMAT)
    is GameBundleImportResult.Failure -> DiagnosticOutcome.failed(code.toDiagnosticCode())
}

private fun GameBundleImportFailureCode.toDiagnosticCode(): DiagnosticCode = when (this) {
    GameBundleImportFailureCode.CREATOR_ID_MISMATCH -> DiagnosticCode.BUNDLE_CREATOR_ID_MISMATCH
    GameBundleImportFailureCode.INVALID_SIGNATURE -> DiagnosticCode.BUNDLE_INVALID_SIGNATURE
    GameBundleImportFailureCode.SIGNATURE_VERIFICATION_FAILED ->
        DiagnosticCode.BUNDLE_SIGNATURE_VERIFICATION_FAILED
    GameBundleImportFailureCode.INVALID_GAME -> DiagnosticCode.BUNDLE_INVALID_GAME
    GameBundleImportFailureCode.PERSISTENCE_FAILED -> DiagnosticCode.PERSISTENCE_FAILED
}

private fun Game.toPortable(publicKey: ByteArray): PortableGame = PortableGame(
    gameId = id.value,
    gameName = name,
    creatorPlayerId = creator.value,
    creatorPublicKey = publicKey.copyOf(),
    things = things.map { thing ->
        PortableThing(
            thingId = thing.id.value,
            clueText = thing.playableClue().clueText,
            targetEmbedding = canonicalImageEmbedding(thing.targetEmbedding),
            matchThreshold = thing.matchThreshold,
        )
    },
)

private fun PortableGame.toImportedGame(): Game = Game(
    id = GameId(gameId),
    name = gameName,
    creator = PlayerId(creatorPlayerId),
    things = things.map { thing ->
        Thing(
            id = ThingId(thing.thingId),
            clueAuthority = ClueAuthority.shared(thing.clueText),
            targetEmbedding = canonicalImageEmbedding(thing.targetEmbedding),
            matchThreshold = thing.matchThreshold,
        )
    },
)

private fun Game.portableContentEquivalent(portable: PortableGame): Boolean {
    if (id.value != portable.gameId || name != portable.gameName || creator.value != portable.creatorPlayerId) {
        return false
    }
    if (things.size != portable.things.size) return false

    return things.indices.all { index ->
        val local = things[index]
        val incoming = portable.things[index]
        local.id.value == incoming.thingId &&
            local.playableClue().clueText == incoming.clueText &&
            local.matchThreshold.toRawBits() == incoming.matchThreshold.toRawBits() &&
            local.targetEmbedding.size == incoming.targetEmbedding.size &&
            local.targetEmbedding.indices.all { embeddingIndex ->
                local.targetEmbedding[embeddingIndex].toRawBits() ==
                    incoming.targetEmbedding[embeddingIndex].toRawBits()
            }
    }
}
