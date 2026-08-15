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
            "p256:0ed3a6ab957ff6f59a9630a473d31a7d04fcb46548b89016196980c7e96584f6",
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
