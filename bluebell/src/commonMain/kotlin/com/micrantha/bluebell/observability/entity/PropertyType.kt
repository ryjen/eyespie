package com.micrantha.bluebell.observability.entity

sealed interface PropertyType {
    object String : PropertyType
    object Number : PropertyType
    object Boolean : PropertyType
    data class Enum(val values: Set<kotlin.String>) : PropertyType
    data class Object(val schema: EventSchema) : PropertyType
}
