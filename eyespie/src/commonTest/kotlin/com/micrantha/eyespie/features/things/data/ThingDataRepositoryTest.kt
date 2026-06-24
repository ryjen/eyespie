package com.micrantha.eyespie.features.things.data

import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.features.things.data.mapping.ThingsDomainMapper
import com.micrantha.eyespie.features.things.data.model.MatchRequest
import com.micrantha.eyespie.features.things.data.model.MatchResponse
import com.micrantha.eyespie.features.things.data.model.NearbyRequest
import com.micrantha.eyespie.features.things.data.model.ThingData
import com.micrantha.eyespie.features.things.data.model.ThingRequest
import com.micrantha.eyespie.features.things.data.model.ThingResponse
import com.micrantha.eyespie.features.things.data.source.ThingsLocalSource
import com.micrantha.eyespie.features.things.data.source.ThingsRemoteSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ThingDataRepositoryTest {

    private class FakeRemoteSource : ThingsRemoteSource {
        var thingsResult: Result<List<ThingData>> = Result.success(emptyList())
        var thingsCalled = 0

        override suspend fun things(playerID: String): Result<List<ThingData>> {
            thingsCalled++
            return thingsResult
        }

        override suspend fun save(data: ThingRequest): Result<ThingResponse> = TODO()
        override suspend fun thing(thingID: String): Result<ThingResponse> = TODO()
        override suspend fun nearby(request: NearbyRequest): Result<List<ThingResponse>> = TODO()
        override suspend fun match(request: MatchRequest): Result<List<MatchResponse>> = TODO()
    }

    private class FakeLocalSource : ThingsLocalSource {
        var savedThings: List<ThingData>? = null
        var getAllResult: Result<List<ThingData>> = Result.success(emptyList())

        override fun saveAll(things: List<ThingData>): Result<Unit> {
            savedThings = things
            return Result.success(Unit)
        }

        override fun getAll(): Result<List<ThingData>> = getAllResult
    }

    private val remoteSource = FakeRemoteSource()
    private val localSource = FakeLocalSource()
    private val mapper = ThingsDomainMapper(LocationDomainMapper())
    private val repository = ThingDataRepository(remoteSource, localSource, mapper)

    @Test
    fun `things should fetch from remote and save to local on success`() = runTest {
        val playerID = "user123"
        val remoteThings = listOf(ThingData(id = "1", imageUrl = "", createdBy = playerID))
        remoteSource.thingsResult = Result.success(remoteThings)

        repository.things(playerID)

        assertTrue(remoteSource.thingsCalled == 1)
        assertTrue(localSource.savedThings == remoteThings)
    }

    @Test
    fun `things should fallback to local when remote fails`() = runTest {
        val playerID = "user123"
        val localThings = listOf(ThingData(id = "1", imageUrl = "", createdBy = playerID))

        remoteSource.thingsResult = Result.failure(Exception("Network error"))
        localSource.getAllResult = Result.success(localThings)

        val result = repository.things(playerID)

        assertTrue(remoteSource.thingsCalled == 1)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.size == 1)
    }
}
