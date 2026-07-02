package com.micrantha.eyespie.core.data.system

import app.cash.turbine.test
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.core.data.system.source.LocationLocalSource
import dev.icerock.moko.geo.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocationDataRepositoryTest {

    private class FakeLocationLocalSource : LocationLocalSource {
        val flow = MutableStateFlow<LatLng?>(null)
        var startTrackingCalled = false
        var stopTrackingCalled = false

        override fun getLocationsFlow(): Flow<LatLng> = flow.filterNotNull()

        override suspend fun startTracking() {
            startTrackingCalled = true
        }

        override fun stopTracking() {
            stopTrackingCalled = true
        }
    }

    private val localSource = FakeLocationLocalSource()
    private val currentSession = CurrentSession
    private val repository = LocationDataRepository(localSource, currentSession)

    @Test
    fun `start should track locations and update currentSession`() = runTest {
        val latLng = LatLng(1.0, 2.0)

        val job = launch {
            repository.start()
        }

        localSource.flow.value = latLng

        repository.flow().test {
            val loc = awaitItem()
            assertEquals(1.0, loc.point.latitude)
            assertEquals(2.0, loc.point.longitude)
        }

        assertTrue(localSource.startTrackingCalled)

        job.cancel()
    }
}
