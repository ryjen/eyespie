package com.micrantha.eyespie.features.onboarding.usecase

import com.micrantha.eyespie.features.onboarding.entities.AiModel

class LoadModelConfig {
     operator fun invoke(): Result<Map<String, AiModel>> = Result.success(
        mapOf(
            "gemma-3n" to AiModel(
                url = "https://dubnium.tail4d84c.ts.net/models/gemma-3n/gemma-3n-E2B-it-int4.litertlm",
                checksum = "6c5f6d8f727e3f4327dbe38731c92c47094a95fccee9c15484465e7d9e01e4d5"
            )
        )
    )
}
