package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.game.LocalGameFailure

data class UtilityContent(
    val identityDisplayName: String,
    val identityIdSuffix: String,
)

data class UtilityState(
    val content: UtilityContent? = null,
    val loading: Boolean = true,
    val failure: LocalGameFailure? = null,
    val loadGeneration: Long = 0,
)
