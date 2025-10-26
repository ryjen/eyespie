package com.micrantha.eyespie.features.onboarding.entities

import com.micrantha.bluebell.domain.security.sha256

data class AiModel(
    val url: String,
    val checksum: String? = null
) {
    val id get() = url.hashCode().toLong()

    fun fileName() = sha256(url)
}
