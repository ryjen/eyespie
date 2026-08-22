package com.micrantha.eyespie.mvi

interface Interactor<State, Intent> : StateReader<State>, IntentDispatcher<Intent>
