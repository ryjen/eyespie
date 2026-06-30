package com.micrantha.eyespie.features.things.data.source

import com.micrantha.eyespie.features.things.data.model.ThingData

internal interface ThingsLocalSource {
    fun getAll(): Result<List<ThingData>>
    fun saveAll(things: List<ThingData>): Result<Unit>
}
