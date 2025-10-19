package com.micrantha.bluebell.plugin

import kotlinx.serialization.Serializable

@Serializable
data class BluebellAssetConfig(
    val downloads: Map<String, Download>,
    val models: Map<String, Model>,
) {
    @Serializable
    data class Model(
        val fileName: String? = null,
    )
    @Serializable
    data class Download(
        val url: String,
        val checksum: String? = null,
    )
}
