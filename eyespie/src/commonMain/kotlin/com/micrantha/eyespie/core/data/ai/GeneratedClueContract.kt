package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.domain.ai.GeneratedClueProvenance
import com.micrantha.eyespie.domain.ai.GeneratedClues
import com.micrantha.eyespie.domain.ai.SemanticInferenceExecutionConfiguration
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.entities.AiClue
import kotlinx.serialization.Serializable

@Serializable
internal data class GeneratedClueResponse(
    val schemaVersion: Int,
    val clues: List<GeneratedClueItem>,
)

@Serializable
internal data class GeneratedClueItem(
    val clue: String,
    val answer: String,
    val confidence: Float,
)

internal sealed class GeneratedClueResponseException(
    val diagnosticCode: String,
) : IllegalArgumentException("generated clue response rejected: $diagnosticCode")

internal class MalformedGeneratedClueResponseException :
    GeneratedClueResponseException("clue_response_malformed")

internal class UnsupportedGeneratedClueSchemaException :
    GeneratedClueResponseException("clue_schema_unsupported")

internal class InvalidGeneratedClueResponseException :
    GeneratedClueResponseException("clue_response_invalid")

internal fun GeneratedClueResponse.validateAndMap(
    identity: SemanticInferenceIdentity,
    executionConfiguration: SemanticInferenceExecutionConfiguration?,
    promptId: String,
    promptVersion: Int,
    repaired: Boolean,
): GeneratedClues {
    if (schemaVersion != CLUE_SCHEMA_VERSION) {
        throw UnsupportedGeneratedClueSchemaException()
    }
    if (clues.size !in 1..MAX_CLUES) {
        throw InvalidGeneratedClueResponseException()
    }

    val proof = clues.map { item ->
        val clue = item.clue.trim()
        val answer = item.answer.trim()
        if (
            clue.isBlank() || answer.isBlank() ||
            clue.length > MAX_CLUE_LENGTH || answer.length > MAX_ANSWER_LENGTH ||
            !item.confidence.isFinite() || item.confidence !in 0f..1f
        ) {
            throw InvalidGeneratedClueResponseException()
        }
        AiClue(
            data = clue,
            confidence = item.confidence,
            answer = answer,
        )
    }.toSet()

    if (proof.size != clues.size) {
        throw InvalidGeneratedClueResponseException()
    }

    return GeneratedClues(
        clues = proof,
        provenance = GeneratedClueProvenance(
            schemaVersion = schemaVersion,
            providerId = identity.providerId,
            runtimeId = identity.runtimeId,
            locality = identity.locality,
            modelId = identity.modelId,
            modelVersion = identity.modelVersion,
            promptId = promptId,
            promptVersion = promptVersion,
            executionConfiguration = executionConfiguration,
            repaired = repaired,
        ),
    )
}

internal const val CLUE_SCHEMA_VERSION = 1
internal const val MAX_CLUES = 3
internal const val MAX_CLUE_LENGTH = 240
internal const val MAX_ANSWER_LENGTH = 120
