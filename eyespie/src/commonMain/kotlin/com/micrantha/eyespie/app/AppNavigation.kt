package com.micrantha.eyespie.app

interface AppNavigation {
    fun push(route: AppRoute)
    fun replace(route: AppRoute)
    fun replaceAll(route: AppRoute)
    fun pop()
}
