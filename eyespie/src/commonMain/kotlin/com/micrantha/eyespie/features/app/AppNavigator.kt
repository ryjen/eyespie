package com.micrantha.eyespie.features.app

import kotlinx.coroutines.flow.StateFlow

interface AppNavigator {
    val route: StateFlow<AppRoute>
    fun navigate(route: AppRoute)
}
