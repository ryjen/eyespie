package com.micrantha.eyespie.features.things.ui.detail

import com.micrantha.bluebell.platform.Serializable
import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.eyespie.domain.entities.Thing
import kotlinx.serialization.Serializable as KSerializable

data class ThingDetailState(
    val status: UiResult<Thing> = UiResult.Default
)

data class ThingDetailUiState(
    val status: UiResult<Thing>
)

@KSerializable
data class ThingDetailArg(
    val id: String
) : Serializable
