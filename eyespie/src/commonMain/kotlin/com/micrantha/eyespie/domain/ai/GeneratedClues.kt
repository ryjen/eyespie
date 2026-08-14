package com.micrantha.eyespie.domain.ai

import com.micrantha.eyespie.domain.entities.AiProof

data class GeneratedClueProvenance(
    val schemaVersion: Int,
    val providerId: String,
    val runtimeId: String,
    val locality: InferenceLocality,
    val modelId: String?,
    val modelVersion: String?,
    val promptId: String,
    val promptVersion: Int,
    val executionConfiguration: SemanticInferenceExecutionConfiguration?,
    val repaired: Boolean,
)

data class GeneratedClues(
    val clues: AiProof,
    val provenance: GeneratedClueProvenance,
)
