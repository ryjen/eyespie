package com.micrantha.eyespie.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalIdentityTest {
    @Test
    fun playerIdIsStableForCanonicalPublicKey() {
        val publicKey = ByteArray(P256_X963_PUBLIC_KEY_SIZE) { index ->
            if (index == 0) P256_X963_UNCOMPRESSED_PREFIX else index.toByte()
        }

        assertEquals(
            "p256:27f77abac3e58bbd9525959b5a1e81f6c3dce99bb06a0609e54f37b8a636fcda",
            playerIdFor(publicKey).value,
        )
        assertEquals(playerIdFor(publicKey), playerIdFor(publicKey.copyOf()))
    }

    @Test
    fun rejectsNonCanonicalPublicKey() {
        assertFailsWith<IllegalArgumentException> {
            playerIdFor(ByteArray(P256_X963_PUBLIC_KEY_SIZE))
        }
        assertFailsWith<IllegalArgumentException> {
            playerIdFor(byteArrayOf(P256_X963_UNCOMPRESSED_PREFIX))
        }
    }
}
