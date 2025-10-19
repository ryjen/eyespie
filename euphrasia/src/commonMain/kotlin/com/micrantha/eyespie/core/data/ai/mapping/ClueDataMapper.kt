package com.micrantha.eyespie.core.data.ai.mapping

import com.micrantha.eyespie.domain.entities.ColorProof
import com.micrantha.eyespie.domain.entities.DetectProof
import com.micrantha.eyespie.domain.entities.LabelProof
import kotlinx.serialization.json.Json

class ClueDataMapper {

    fun toColorProof(json: String): ColorProof {
        return Json.decodeFromString(json)
    }

    fun toDetectProof(json: String): DetectProof {
        return Json.decodeFromString(json)
    }

    fun toLabelProof(json: String): LabelProof {
        return Json.decodeFromString(json)
    }
}
