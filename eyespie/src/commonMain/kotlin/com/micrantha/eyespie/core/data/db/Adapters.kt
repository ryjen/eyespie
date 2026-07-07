package com.micrantha.eyespie.core.data.db

import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
val instantAdapter = object : ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant = Instant.parse(databaseValue)
    override fun encode(value: Instant): String = value.toString()
}

class JsonElementAdapter(private val json: Json) : ColumnAdapter<JsonElement, String> {
    override fun decode(databaseValue: String): JsonElement = json.parseToJsonElement(databaseValue)
    override fun encode(value: JsonElement): String = json.encodeToString(JsonElement.serializer(), value)
}
