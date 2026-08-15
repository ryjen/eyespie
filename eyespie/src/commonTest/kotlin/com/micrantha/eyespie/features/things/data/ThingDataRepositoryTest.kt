package com.micrantha.eyespie.features.things.data

import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.toCanonicalEmbedding
import com.micrantha.eyespie.features.things.data.mapping.ThingsDomainMapper
import com.micrantha.eyespie.features.things.data.model.MatchRequest
import com.micrantha.eyespie.features.things.data.model.MatchResponse
import com.micrantha.eyespie.features.things.data.model.NearbyRequest
import com.micrantha.eyespie.features.things.data.model.ThingAuthorityData
import com.micrantha.eyespie.features.things.data.model.ThingListing
import com.micrantha.eyespie.features.things.data.model.ThingRequest
import com.micrantha.eyespie.features.things.data.model.ThingResponse
import com.micrantha.eyespie.features.things.data.source.ThingsLocalSource
import com.micrantha.eyespie.features.things.data.source.ThingsRemoteSource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThingDataRepositoryTest {

    private class FakeThingsRemoteSource : ThingsRemoteSource {
        var thingsResult: Result<List<ThingListing>> = Result.success(emptyList())
        var saveResult: Result<ThingResponse> = Result.failure(Exception("Not implemented"))
        var thingResult: Result<ThingResponse> = Result.failure(Exception("Not found"))
        var nearbyResult: Result<List<ThingListing>> = Result.success(emptyList())
        var matchResult: Result<MatchResponse> = Result.failure(Exception("Not configured"))
        var matchRequest: MatchRequest? = null

        override suspend fun save(data: ThingRequest) = saveResult
        override suspend fun things(playerID: String) = thingsResult
        override suspend fun thing(thingID: String) = thingResult
        override suspend fun nearby(request: NearbyRequest) = nearbyResult
        override suspend fun match(request: MatchRequest): Result<MatchResponse> {
            matchRequest = request
            return matchResult
        }
    }

    private class FakeThingsLocalSource : ThingsLocalSource {
        var things: List<ThingAuthorityData> = emptyList()
        var saveAllCalledWith: List<ThingAuthorityData>? = null

        override fun getAll(): Result<List<ThingAuthorityData>> = Result.success(things)

        override fun saveAll(things: List<ThingAuthorityData>): Result<Unit> {
            saveAllCalledWith = things
            this.things = things
            return Result.success(Unit)
        }
    }

    private val remoteSource = FakeThingsRemoteSource()
    private val localSource = FakeThingsLocalSource()
    private val mapper = ThingsDomainMapper(LocationDomainMapper())
    private val repository = ThingDataRepository(remoteSource, localSource, mapper)

    @Test
    fun `safe things listing should not hydrate authority cache`() = runTest {
        val playerID = "user123"
        remoteSource.thingsResult = Result.success(
            listOf(ThingListing(id = "1", createdAt = "2026-08-14T00:00:00Z", guessed = false))
        )

        val result = repository.things(playerID).toList().single().getOrThrow()

        assertEquals(listOf("1"), result.map { it.id })
        assertNull(localSource.saveAllCalledWith)
    }

    @Test
    fun `full thing authority should never be emitted from unscoped local cache`() = runTest {
        localSource.things = listOf(
            ThingAuthorityData(
                id = "target-1",
                createdBy = "previous-account-player",
                imagePath = "previous-account-player/private.png",
            )
        )
        remoteSource.thingResult = Result.failure(IllegalStateException("not authorized"))

        val results = repository.thing("target-1").toList()

        assertEquals(1, results.size)
        assertTrue(results.single().isFailure)
    }

    @Test
    fun `match should send canonical embedding only for explicit target`() = runTest {
        val embedding = List(ImageEmbeddingContract.dimensions) { index ->
            if (index == 0) 1f else 0f
        }.toCanonicalEmbedding()
        remoteSource.matchResult = Result.success(
            MatchResponse(id = "target-1", similarity = 0.91f, matched = true)
        )

        val results = repository.match("target-1", embedding).toList()

        val match = results.single().getOrThrow()
        assertEquals("target-1", match.id)
        assertEquals(0.91f, match.similarity)
        assertTrue(match.matched)

        val request = remoteSource.matchRequest!!
        assertEquals("target-1", request.thingID)
        assertEquals(ImageEmbeddingContract.dimensions, request.embedding.size)
        assertEquals(1f, request.embedding.first())
        assertTrue(request.embedding.drop(1).all { it == 0f })
        assertEquals(mapper.matchThreshold, request.threshold)
    }
}
