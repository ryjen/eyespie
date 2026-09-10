package com.micrantha.eyespie.app

import android.content.Intent
import android.net.Uri
import com.micrantha.eyespie.core.GameId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidExternalAppIntentSourceTest {
    @Test
    fun canonical_local_game_link_is_accepted() {
        val source = AndroidExternalAppIntentSource()

        assertTrue(source.offer(viewIntent("eyespie://game/game:1234-abcd")))
        assertEquals("game:1234-abcd", source.pendingGameIdState())
    }

    @Test
    fun latest_pending_deep_link_wins_and_acknowledgement_commits_it() = runTest {
        val source = AndroidExternalAppIntentSource()

        assertTrue(source.offer(viewIntent("eyespie://game/game:first")))
        assertTrue(source.offer(viewIntent("eyespie://game/game:second")))

        val pending = source.intents.first()
        assertEquals(
            ExternalAppIntent.OpenLocalGame(GameId("game:second")),
            pending,
        )
        assertEquals("game:second", source.pendingGameIdState())

        source.acknowledge(pending)
        assertNull(source.pendingGameIdState())
    }

    @Test
    fun pending_game_id_can_be_restored_after_recreation() = runTest {
        val source = AndroidExternalAppIntentSource()

        assertTrue(source.restorePendingGameId("game:restored"))
        assertEquals(
            ExternalAppIntent.OpenLocalGame(GameId("game:restored")),
            source.intents.first(),
        )
        assertEquals("game:restored", source.pendingGameIdState())
        assertFalse(source.restorePendingGameId("bad?query"))
    }

    @Test
    fun query_fragment_userinfo_and_port_are_rejected() {
        assertFalse(AndroidExternalAppIntentSource().offer(viewIntent("eyespie://game/game:1234?import=true")))
        assertFalse(AndroidExternalAppIntentSource().offer(viewIntent("eyespie://game/game:1234#fragment")))
        assertFalse(AndroidExternalAppIntentSource().offer(viewIntent("eyespie://user@game/game:1234")))
        assertFalse(AndroidExternalAppIntentSource().offer(viewIntent("eyespie://game:1234/game:1234")))
    }

    private fun viewIntent(uri: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
}
