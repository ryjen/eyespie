package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.TelemetryEvent

class PropertyMapper {
    private val mappings = mutableMapOf<Pair<Destination, String>, PropertyMapping>()

    fun registerMapping(
        destination: Destination,
        eventType: String,
        mapping: PropertyMapping
    ) {
        mappings[destination to eventType] = mapping
    }

    fun map(event: TelemetryEvent, destination: Destination): Map<String, Any> {
        val mapping = mappings[destination to event::class.simpleName]
            ?: return event.properties

        return mapping.transform(event.properties)
    }
}
