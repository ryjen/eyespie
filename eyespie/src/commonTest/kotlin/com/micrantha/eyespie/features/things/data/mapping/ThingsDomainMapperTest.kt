package com.micrantha.eyespie.features.things.data.mapping

import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.floats
import com.micrantha.eyespie.domain.entities.toCanonicalEmbedding
import com.micrantha.eyespie.domain.entities.toPostgresVector
import com.micrantha.eyespie.features.things.data.model.MatchResponse
import com.micrantha.eyespie.features.things.data.model.ThingListing
import com.micrantha.eyespie.features.things.data.model.ThingResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThingsDomainMapperTest {
    private val mapper = ThingsDomainMapper(LocationDomainMapper())
    private val embedding = List(ImageEmbeddingContract.dimensions) { index ->
        when (index) {
            0 -> 1f
            1 -> -0.5f
            else -> 0f
        }
    }.toCanonicalEmbedding()

    @Test
    fun `owner authority pgvector maps to canonical domain embedding`() {
        val thing = mapper.map(
            ThingResponse(
                id = "thing-1",
                imagePath = "player-1/image.png",
                createdBy = "player-1",
                embedding = embedding.toPostgresVector(),
            )
        )

        assertEquals("player-1/image.png", thing.imagePath)
        assertEquals(embedding, thing.embedding)
        assertEquals(ImageEmbeddingContract.dimensions, thing.embedding!!.floats().size)
    }

    @Test
    fun `owner authority rejects rows without durable image path`() {
        assertFailsWith<IllegalArgumentException> {
            mapper.map(
                ThingResponse(
                    id = "thing-1",
                    imagePath = null,
                    createdBy = "player-1",
                )
            )
        }
    }

    @Test
    fun `new thing request preserves opaque image path and canonical pgvector`() {
        val request = mapper.new(
            proof = Proof(clues = null, location = null, embedding = embedding),
            imagePath = "player-1/image.png",
            playerId = "player-1",
        )

        assertEquals("player-1/image.png", request.imagePath)
        assertEquals(embedding.toPostgresVector(), request.embedding)
    }

    @Test
    fun `safe listing maps no authority-only fields`() {
        val listing = mapper.list(
            ThingListing(
                id = "thing-1",
                createdAt = "2026-08-14T00:00:00Z",
                guessed = false,
            )
        )

        assertEquals("thing-1", listing.id)
        assertFalse(listing.guessed)
    }

    @Test
    fun `target match contains only id similarity and backend decision`() {
        val match = mapper.match(
            MatchResponse(
                id = "thing-1",
                similarity = 0.92f,
                matched = true,
            )
        )

        assertEquals("thing-1", match.id)
        assertEquals(0.92f, match.similarity)
        assertTrue(match.matched)
    }
}
