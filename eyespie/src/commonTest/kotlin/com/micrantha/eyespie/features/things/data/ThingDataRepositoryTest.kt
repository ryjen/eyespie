package com.micrantha.eyespie.features.things.data

import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.domain.entities.ALPHA_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.EmbeddingMetadata
import com.micrantha.eyespie.domain.entities.EmbeddingNormalization
import com.micrantha.eyespie.domain.entities.EmbeddingSimilarity
import com.micrantha.eyespie.features.things.data.mapping.ThingsDomainMapper
import com.micrantha.eyespie.features.things.data.model.MatchRequest
import com.micrantha.eyespie.features.things.data.model.MatchResponse
import com.micrantha.eyespie.features.things.data.model.NearbyRequest
import com.micrantha.eyespie.features.things.data.model.ThingData
import com.micrantha.eyespie.features.things.data.model.ThingListing
import com.micrantha.eyespie.features.things.data.model.ThingRequest
import com.micrantha.eyespie.features.things.data.model.ThingResponse
import com.micrantha.eyespie.features.things.data.source.ThingsLocalSource
import com.micrantha.eyespie.features.things.data.source.ThingsRemoteSource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThingDataRepositoryTest {

    private class FakeThingsRemoteSource : ThingsRemoteSource {
        var thingsResult: Result<List<ThingListing>> = Result.success(emptyList())
        var saveResult: Result<ThingResponse> = Result.failure(Exception("Not implemented"))
        var thingResult: Result<ThingResponse> = Result.failure(Exception("Not found"))
        var nearbyResult: Result<List<ThingResponse>> = Result.success(emptyList())
        var matchResult: Result<List<MatchResponse>> = Result.success(emptyList())

        override suspend fun save(data: ThingRequest) = saveResult
        override suspend fun things(playerID: String) = thingsResult
        override suspend fun thing(thingID: String) = thingResult
        override suspend fun nearby(request: NearbyRequest) = nearbyResult
        override suspend fun match(request: MatchRequest) = matchResult
    }

    private class FakeThingsLocalSource : ThingsLocalSource {
        var things: List<ThingData> = emptyList()
        var saveAllCalledWith: List<ThingData>? = null

        override fun getAll(): Result<List<ThingData>> = Result.success(things)

        override fun saveAll(things: List<ThingData>): Result<Unit> {
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
    fun `things should fetch from remote and save to local on success`() = runTest {
        val playerID = "user123"
        val remoteThings = listOf(ThingData(id = "1", imageUrl = "", createdBy = playerID))
        remoteSource.thingsResult = Result.success(remoteThings)

        repository.things(playerID).toList()

        assertEquals(remoteThings, localSource.saveAllCalledWith)
    }

    @Test
    fun `match should return compatible local results first`() = runTest {
        val embedding = testEmbedding(METADATA, 1f)
        localSource.things = listOf(
            ThingData(
                id = "1",
                imageUrl = "",
                createdBy = "u1",
                embedding = embedding.toPgVector(),
                embeddingModelId = embedding.metadata.persistenceId,
            )
        )

        val results = repository.match(embedding).toList()

        assertTrue(results.any { it.isSuccess })
        val matchResult = results.first { it.isSuccess }.getOrThrow()
        assertEquals(1, matchResult.size)
        assertEquals("1", matchResult.first().id)
    }

    @Test
    fun `match ignores same-dimension vectors from another model`() = runTest {
        val embedding = testEmbedding(METADATA, 1f)
        val other = testEmbedding(METADATA.copy(modelId = "test:model:v2"), 1f)
        localSource.things = listOf(
            ThingData(
                id = "1",
                imageUrl = "",
                createdBy = "u1",
                embedding = other.toPgVector(),
                embeddingModelId = other.metadata.persistenceId,
            )
        )

        val firstResult = repository.match(embedding).toList().first().getOrThrow()

        assertTrue(firstResult.isEmpty())
    }

    private fun testEmbedding(metadata: EmbeddingMetadata, first: Float): Embedding = Embedding.of(
        metadata,
        List(ALPHA_EMBEDDING_DIMENSIONS) { index -> if (index == 0) first else 0f },
    )

    private companion object {
        val METADATA = EmbeddingMetadata(
            modelId = "test:model:v1",
            dimensions = ALPHA_EMBEDDING_DIMENSIONS,
            normalization = EmbeddingNormalization.ModelDefined,
            similarity = EmbeddingSimilarity.Cosine,
        )
    }
}
