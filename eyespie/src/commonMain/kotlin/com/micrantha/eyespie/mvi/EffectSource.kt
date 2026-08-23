package com.micrantha.eyespie.mvi

import kotlinx.coroutines.flow.Flow

interface EffectSource<Effect> {
    val effects: Flow<Effect>
}
