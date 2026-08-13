package com.micrantha.eyespie.features.things.data.source

import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.domain.entities.toPostgresEmbedding
import com.micrantha.eyespie.domain.entities.toPostgresVector
import com.micrantha.eyespie.features.things.data.model.ThingData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okio.ByteString.Companion.toByteString

internal class SqlThingsLocalSource(
    database: EyesPieDatabase,
    private val json: Json
) : ThingsLocalSource {
    private val queries = database.eyesPieQueries

    override fun getAll(): Result<List<ThingData>> = try {
        val things =
            queries.selectAllThings { id, created_by, image_url, created_at, location, proof, embedding ->
                ThingData(
                    id = id,
                    createdBy = created_by,
                    imageUrl = image_url,
                    createdAt = created_at,
                    location = location,
                    proof = proof?.let { json.parseToJsonElement(it) },
                    embedding = embedding?.toByteString()?.toPostgresVector()
                )
            }.executeAsList()
        Result.success(things)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun saveAll(things: List<ThingData>): Result<Unit> = try {
        queries.transaction {
            things.forEach { thing ->
                queries.insertThing(
                    id = thing.id!!,
                    created_by = thing.createdBy,
                    image_url = thing.imageUrl,
                    created_at = thing.createdAt!!,
                    location = thing.location,
                    clues = thing.proof?.let { json.encodeToString(JsonElement.serializer(), it) },
                    embedding = thing.embedding?.toPostgresEmbedding()?.toByteArray()
                )
            }
        }
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
