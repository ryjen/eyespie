package com.micrantha.eyespie.features.utility

import kotlinx.coroutines.CoroutineScope

class UtilityFactory(
    private val loader: UtilityLoader,
    private val output: (UtilityOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        initialState: UtilityState = UtilityState(),
    ): UtilityInteractor = UtilityInteractor(
        loader = loader,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
