package com.micrantha.eyespie.core.data.system.source

import dev.icerock.moko.geo.LatLng
import kotlinx.coroutines.flow.Flow

internal interface LocationLocalSource {
    fun getLocationsFlow(): Flow<LatLng>
    suspend fun startTracking()
    fun stopTracking()
}
