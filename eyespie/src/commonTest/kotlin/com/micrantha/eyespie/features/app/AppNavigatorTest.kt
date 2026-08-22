package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testThingId
import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigatorTest {
    @Test
    fun navigate_replaces_current_route() {
        val navigator = StateFlowAppNavigator()
        val play = AppRoute.Play(testGameId, testThingId)

        navigator.navigate(play)

        assertEquals(play, navigator.route.value)
    }
}
