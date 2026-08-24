package com.micrantha.eyespie.features.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppNavigationBridgeTest {
    @Test
    fun forwards_only_while_navigation_is_attached() {
        val bridge = AppNavigationBridge()
        val navigation = RecordingBridgeNavigation()

        assertFailsWith<IllegalStateException> { bridge.push(AppRoute.Home) }

        bridge.attach(navigation)
        bridge.push(AppRoute.Utility)
        bridge.replace(AppRoute.Home)
        bridge.pop()
        bridge.detach(navigation)

        assertEquals(listOf("push:Utility", "replace:Home", "pop"), navigation.commands)
        assertFailsWith<IllegalStateException> { bridge.pop() }
    }
}

private class RecordingBridgeNavigation : AppNavigation {
    val commands = mutableListOf<String>()

    override fun push(route: AppRoute) {
        commands += "push:${route::class.simpleName}"
    }

    override fun replace(route: AppRoute) {
        commands += "replace:${route::class.simpleName}"
    }

    override fun replaceAll(route: AppRoute) {
        commands += "replaceAll:${route::class.simpleName}"
    }

    override fun pop() {
        commands += "pop"
    }
}
