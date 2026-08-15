package com.micrantha.eyespie.identity

import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import okio.ByteString.Companion.toByteString

/**
 * Platform-backed signing identity.
 *
 * Implementations must keep private key material non-exportable. Public keys use
 * the canonical ANSI X9.63 uncompressed P-256 point representation:
 * 0x04 || X(32 bytes) || Y(32 bytes).
 */
interface SigningIdentity {
    suspend fun publicKey(): ByteArray
    suspend fun sign(payload: ByteArray): ByteArray
    suspend fun verify(publicKey: ByteArray, payload: ByteArray, signature: ByteArray): Boolean
}

expect class PlatformSigningIdentity() : SigningIdentity

class LocalPlayerIdentityRepository(
    private val signingIdentity: SigningIdentity = PlatformSigningIdentity(),
    private val displayName: String = DEFAULT_DISPLAY_NAME,
) : PlayerIdentityRepository {
    init {
        require(displayName.isNotBlank()) { "display name must not be blank" }
    }

    override suspend fun current(): PlayerIdentity = PlayerIdentity(
        id = playerIdFor(signingIdentity.publicKey()),
        displayName = displayName,
    )

    companion object {
        const val DEFAULT_DISPLAY_NAME = "Agent"
    }
}

fun playerIdFor(publicKey: ByteArray): PlayerId {
    require(publicKey.size == P256_X963_PUBLIC_KEY_SIZE) {
        "P-256 public key must be a 65-byte X9.63 uncompressed point"
    }
    require(publicKey.first() == P256_X963_UNCOMPRESSED_PREFIX) {
        "P-256 public key must use X9.63 uncompressed-point encoding"
    }

    val digest = publicKey.toByteString().sha256().hex()
    return PlayerId("p256:$digest")
}

internal const val P256_X963_PUBLIC_KEY_SIZE = 65
internal const val P256_X963_UNCOMPRESSED_PREFIX: Byte = 0x04
