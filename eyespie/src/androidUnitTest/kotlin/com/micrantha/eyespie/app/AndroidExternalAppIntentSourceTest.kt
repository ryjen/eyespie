package com.micrantha.eyespie.app

import android.content.Intent
import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidExternalAppIntentSourceTest {
    @Test
    fun canonical_local_game_link_is_accepted() {
        val source = AndroidExternalAppIntentSource()

        assertTrue(source.offer(viewIntent("eyespie://game/game:1234-abcd")))
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
