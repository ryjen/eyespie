package com.micrantha.eyespie.features.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StateFlowAppNavigator(
    initialRoute: AppRoute = AppRoute.Home,
) : AppNavigator {
    private val mutableRoute = MutableStateFlow(initialRoute)
    override val route: StateFlow<AppRoute> = mutableRoute.asStateFlow()

    override fun navigate(route: AppRoute) {
        mutableRoute.value = route
    }
}
