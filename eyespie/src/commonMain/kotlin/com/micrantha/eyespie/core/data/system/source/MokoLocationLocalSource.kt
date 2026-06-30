package com.micrantha.eyespie.core.data.system.source

import dev.icerock.moko.geo.LocationTracker

internal class MokoLocationLocalSource(
    private val tracker: LocationTracker
) : LocationLocalSource {
    override fun getLocationsFlow() = tracker.getLocationsFlow()
    override suspend fun startTracking() = tracker.startTracking()
    override fun stopTracking() = tracker.stopTracking()
}
