package com.micrantha.eyespie.mvi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class EffectEmitter<Effect> : EffectSource<Effect> {
    private val channel = Channel<Effect>(Channel.BUFFERED)

    override val effects: Flow<Effect> = channel.receiveAsFlow()

    fun emit(effect: Effect) {
        check(channel.trySend(effect).isSuccess) { "presentation effect could not be queued" }
    }
}
