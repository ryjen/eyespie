package com.micrantha.eyespie.core.data.ai.source

import com.micrantha.bluebell.domain.security.hash
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.domain.entities.ModelInfo

class ModelSource(private val platform: Platform) {
    // TODO: get from supabase or app configuration
    val modelInfo = listOf(
        ModelInfo(
            url = "https://huggingface.co/unsloth/gemma-3-270m-it-GGUF/resolve/main/gemma-3-270m-it-Q8_0.gguf?download=true",
            name = "Gemma3",
            checksum = "d156a5159f2f79c1b1d53c7c1cc20f1ff28ab8d00f17a292620aad13399b9698"
        )
    )

    fun exists(model: ModelInfo) = platform.fileExists(platform.filePath(hash(model.name)))
}
