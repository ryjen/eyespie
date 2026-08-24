package com.micrantha.eyespie.app

internal class AppNavigationBridge : AppNavigation {
    private var delegate: AppNavigation? = null

    fun attach(navigation: AppNavigation) {
        check(delegate == null || delegate === navigation) { "app navigation is already attached" }
        delegate = navigation
    }

    fun detach(navigation: AppNavigation) {
        if (delegate === navigation) delegate = null
    }

    override fun push(route: AppRoute) = active().push(route)
    override fun replace(route: AppRoute) = active().replace(route)
    override fun replaceAll(route: AppRoute) = active().replaceAll(route)
    override fun pop() = active().pop()

    private fun active(): AppNavigation = checkNotNull(delegate) { "app navigation is not attached" }
}
