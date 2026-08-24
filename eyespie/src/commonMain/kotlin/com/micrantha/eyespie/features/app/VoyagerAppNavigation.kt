package com.micrantha.eyespie.features.app

import cafe.adriel.voyager.navigator.Navigator

internal class VoyagerAppNavigation(
    private val navigator: Navigator,
) : AppNavigation {
    override fun push(route: AppRoute) {
        navigator.push(route.toDestination())
    }

    override fun replace(route: AppRoute) {
        navigator.replace(route.toDestination())
    }

    override fun replaceAll(route: AppRoute) {
        navigator.replaceAll(route.toDestination())
    }

    override fun pop() {
        navigator.pop()
    }
}
