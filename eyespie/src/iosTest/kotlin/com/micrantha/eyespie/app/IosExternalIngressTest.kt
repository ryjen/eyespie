package com.micrantha.eyespie.app

import com.micrantha.eyespie.core.GameId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSURL

class IosExternalIngressTest {
    @Test
    fun eyespie_file_url_is_queued_until_acknowledged() {
        IosExternalIngress.acknowledgePendingDocument()
        val url = NSURL.fileURLWithPath("/tmp/shared-mission.eyespie")

        assertTrue(IosExternalIngress.offerDocument(url))
        assertTrue(IosExternalIngress.pending.value)
        assertEquals(url, IosExternalIngress.pendingDocumentUrl())

        IosExternalIngress.acknowledgePendingDocument()
        assertFalse(IosExternalIngress.pending.value)
    }

    @Test
    fun unrelated_file_extension_is_rejected() {
        IosExternalIngress.acknowledgePendingDocument()

        assertFalse(
            IosExternalIngress.offerDocument(
                NSURL.fileURLWithPath("/tmp/archive.bin"),
            ),
        )
    }

    @Test
    fun canonical_local_game_link_is_queued_until_acknowledged() = runTest {
        val expected = ExternalAppIntent.OpenLocalGame(GameId("game:1234-abcd"))

        assertTrue(
            IosExternalIngress.offerDeepLink(
                scheme = "eyespie",
                host = "game",
                pathSegments = listOf("game:1234-abcd"),
                hasQuery = false,
                hasFragment = false,
                hasUserInfo = false,
                hasPort = false,
            ),
        )
        assertEquals(expected, IosExternalIngress.intents.first())

        IosExternalIngress.acknowledge(expected)
    }

    @Test
    fun decorated_deep_links_are_rejected() {
        assertFalse(
            IosExternalIngress.offerDeepLink(
                scheme = "eyespie",
                host = "game",
                pathSegments = listOf("game:1234"),
                hasQuery = true,
                hasFragment = false,
                hasUserInfo = false,
                hasPort = false,
            ),
        )
        assertFalse(
            IosExternalIngress.offerDeepLink(
                scheme = "eyespie",
                host = "game",
                pathSegments = listOf("game:1234"),
                hasQuery = false,
                hasFragment = false,
                hasUserInfo = true,
                hasPort = false,
            ),
        )
    }
}
