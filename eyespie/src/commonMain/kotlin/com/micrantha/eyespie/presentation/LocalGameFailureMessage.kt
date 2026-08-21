package com.micrantha.eyespie.presentation

import com.micrantha.eyespie.game.LocalGameFailure

fun localGameFailureMessage(failure: LocalGameFailure): String = failure.message ?: failure.code.name
