package com.micrantha.eyespie.features.things.data.mapping

import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.floats
import com.micrantha.eyespie.domain.entities.toCanonicalEmbedding
import com.micrantha.eyespie.domain.entities.toPostgresVector
import com.micrantha.eyespie.features.things.data.model.MatchResponse
import com.micrantha.eyespie.features.things.data.model.ThingResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun `remote pgvector maps to canonical domain embedding`() {
        val thing = mapper.map(
            ThingResponse(
                id = "thing-1",
                imageUrl = "https://example.invalid/image.png",
                createdBy = "player-1",
                embedding = embedding.toPostgresVector(),
            )
        )

        assertEquals(embedding, thing.embedding)
        assertEquals(ImageEmbeddingContract.dimensions, thing.embedding!!.floats().size)
    }

    @Test
    fun `new thing request preserves canonical pgvector representation`() {
        val request = mapper.new(
            proof = Proof(clues = null, location = null, embedding = embedding),
            imageUrl = "https://example.invalid/image.png",
            playerId = "player-1",
        )

        assertEquals(embedding.toPostgresVector(), request.embedding)
    }

    @Test
    fun `match RPC decodes embedding from returned Thing content`() {
        val content = ThingResponse(
            id = "thing-1",
            imageUrl = "https://example.invalid/image.png",
            createdBy = "player-1",
            embedding = embedding.toPostgresVector(),
        )

        val match = mapper.match(
            MatchResponse(
                id = "thing-1",
                content = Json.encodeToJsonElement(content),
                similarity = 0.92f,
            )
        )

        assertEquals("thing-1", match.id)
        assertEquals(embedding, match.embedding)
        assertEquals(0.92f, match.similarity)
    }

    @Test
    fun `match RPC rejects missing embedding or inconsistent id`() {
        assertFailsWith<IllegalArgumentException> {
            mapper.match(
                MatchResponse(
                    id = "thing-1",
                    content = Json.encodeToJsonElement(
                        ThingResponse(
                            id = "thing-1",
                            imageUrl = "https://example.invalid/image.png",
                            createdBy = "player-1",
                            embedding = null,
                        )
                    ),
                    similarity = 0.92f,
                )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            mapper.match(
                MatchResponse(
                    id = "thing-1",
                    content = Json.encodeToJsonElement(
                        ThingResponse(
                            id = "thing-2",
                            imageUrl = "https://example.invalid/image.png",
                            createdBy = "player-1",
                            embedding = embedding.toPostgresVector(),
                        )
                    ),
                    similarity = 0.92f,
                )
            )
        }
    }
}
