package com.micrantha.eyespie.identity

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
