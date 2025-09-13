package com.micrantha.eyespie.domain.entities

data class UrlFile(
    val location: String,
    val checksum: String // sha256 hash of the file
)

data class ModelInfo(
    val model: UrlFile,
    val encoder: UrlFile,
    val name: String,
)
