package com.micrantha.eyespie.features.utility

import kotlinx.coroutines.CoroutineScope

class UtilityFactory(
    private val port: UtilityPort,
    private val output: (UtilityOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        initialState: UtilityState = UtilityState(),
    ): UtilityInteractor = UtilityInteractor(
        port = port,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
