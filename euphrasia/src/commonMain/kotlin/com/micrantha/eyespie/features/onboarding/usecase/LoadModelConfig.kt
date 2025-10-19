package com.micrantha.eyespie.features.onboarding.usecase

import com.micrantha.eyespie.features.onboarding.entities.AiModel

class LoadModelConfig {
     operator fun invoke(): Result<Map<String, AiModel>> = Result.success(
        mapOf(
            "gemma-3n" to AiModel(
                url = "https://dubnium.tail4d84c.ts.net/models/gemma-3n/gemma-3n-E2B-it-int4.task",
                checksum = "A7F544CFEE68F579FABADB22AA9284FAA4020A0F5358D0E15B49FDD4CEFE4200"
            )
        )
    )
}
