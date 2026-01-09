package com.micrantha.bluebell.observability.entity

data class ValidationWarning(
    val field: String,
    val message: String,
    val suggestion: String?
)
