package com.micrantha.eyespie.domain.entities

data class UrlFile(
    val location: String,
    val checksum: String // sha256 hash of the file
)

data class ModelFile(
    val downloadUrl: String,
    val name: String,
    val slug: String,
)

data class ModelInfo(
    val model: UrlFile,
    val name: String,
)
