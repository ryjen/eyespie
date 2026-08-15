package com.micrantha.eyespie.features.things.data.source

import com.micrantha.eyespie.features.things.data.model.ThingAuthorityData

internal interface ThingsLocalSource {
    fun getAll(): Result<List<ThingAuthorityData>>
    fun saveAll(things: List<ThingAuthorityData>): Result<Unit>
}
