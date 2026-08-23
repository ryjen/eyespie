package com.micrantha.eyespie.mvi

interface IntentDispatcher<Intent> {
    fun dispatch(intent: Intent)
}
