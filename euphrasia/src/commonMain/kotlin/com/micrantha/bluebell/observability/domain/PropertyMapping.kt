package com.micrantha.bluebell.observability.domain

data class PropertyMapping(
    val fieldMappings: Map<String, String>, // source -> destination
    val transformations: Map<String, (Any) -> Any> = emptyMap(),
    val filters: Map<String, (Any) -> Boolean> = emptyMap()
) {
    fun transform(properties: Map<String, Any>): Map<String, Any> {
        return properties
            .mapKeys { (key, _) -> fieldMappings[key] ?: key }
            .mapValues { (key, value) ->
                transformations[key]?.invoke(value) ?: value
            }
            .filter { (key, value) ->
                filters[key]?.invoke(value) != false
            }
    }
}
