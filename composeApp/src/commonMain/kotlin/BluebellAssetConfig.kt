package com.micrantha.bluebell

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@Serializable
data class BluebellAssetConfig(
    val downloads: Map<String, Download>,
) {
    @Serializable
    data class Download(
        val url: String? = null,
        val androidUrl: String? = null,
        val iosUrl: String? = null,
        val checksum: String? = null,
    )
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun load(manifest: String): BluebellAssetConfig? {
            return BluebellAssetConfig::class.java.getResourceAsStream(manifest)?.use {
                Json.decodeFromStream<BluebellAssetConfig>(it)
            }
        }
    }
}
