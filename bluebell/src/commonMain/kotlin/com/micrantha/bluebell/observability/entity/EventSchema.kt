package com.micrantha.bluebell.observability.entity

data class EventSchema(
    val name: String,
    val version: Int,
    val properties: Map<String, PropertyType>,
    val required: Set<String>,
    val deprecated: Boolean = false,
    val deprecatedFields: Set<String>? = null,
    val fieldReplacements: Map<String, String>? = null,
    val description: String? = null
)

fun EventSchema.toVersion() = SchemaVersion(
    name, version
)
