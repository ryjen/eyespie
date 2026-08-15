package com.micrantha.eyespie.clue

enum class ClueOrigin {
    LEGACY,
    MANUAL,
    GENERATED,
}

data class GeneratedClueProvenance(
    val providerId: String,
    val modelId: String,
    val confidence: Double? = null,
) {
    init {
        require(providerId.isNotBlank() && providerId == providerId.trim()) {
            "provider id must be non-blank and trimmed"
        }
        require(modelId.isNotBlank() && modelId == modelId.trim()) {
            "model id must be non-blank and trimmed"
        }
        require(providerId.length <= MAX_PROVENANCE_ID_LENGTH) { "provider id is too long" }
        require(modelId.length <= MAX_PROVENANCE_ID_LENGTH) { "model id is too long" }
        require(confidence == null || (confidence.isFinite() && confidence in 0.0..1.0)) {
            "confidence must be finite and between zero and one"
        }
    }

    companion object {
        const val MAX_PROVENANCE_ID_LENGTH: Int = 160
    }
}

data class PlayableClue(
    val clueText: String,
)

enum class ClueValidationError {
    BLANK_CLUE,
    CLUE_TOO_LONG,
    BLANK_EXPECTED_ANSWER,
    EXPECTED_ANSWER_TOO_LONG,
}

sealed interface ClueAuthoringResult {
    data class Accepted(val authority: ClueAuthority) : ClueAuthoringResult
    data class Rejected(val error: ClueValidationError) : ClueAuthoringResult
}

data class ClueAuthority private constructor(
    val schemaVersion: Int,
    val clueText: String,
    val expectedAnswer: String?,
    val origin: ClueOrigin,
    val generatedProvenance: GeneratedClueProvenance?,
) {
    init {
        require(schemaVersion == CURRENT_SCHEMA_VERSION) { "unsupported clue authority schema version" }
        require(clueText.isNotBlank()) { "clue text must not be blank" }
        require(clueText.length <= MAX_CLUE_LENGTH) { "clue text is too long" }
        require(clueText == normalize(clueText)) { "clue text must be normalized" }

        when (origin) {
            ClueOrigin.LEGACY -> {
                require(expectedAnswer == null) { "legacy clue authority cannot invent an expected answer" }
                require(generatedProvenance == null) { "legacy clue authority cannot contain generated provenance" }
            }
            ClueOrigin.MANUAL -> {
                requireValidExpectedAnswer(expectedAnswer)
                require(generatedProvenance == null) { "manual clue authority cannot contain generated provenance" }
            }
            ClueOrigin.GENERATED -> {
                requireValidExpectedAnswer(expectedAnswer)
                require(generatedProvenance != null) { "generated clue authority requires generated provenance" }
            }
        }
    }

    fun playable(): PlayableClue = PlayableClue(clueText = clueText)

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
        const val MAX_CLUE_LENGTH: Int = 280
        const val MAX_EXPECTED_ANSWER_LENGTH: Int = 160

        fun manual(clueText: String, expectedAnswer: String): ClueAuthoringResult {
            val normalizedClue = normalize(clueText)
            val normalizedAnswer = normalize(expectedAnswer)
            validationError(normalizedClue, normalizedAnswer)?.let {
                return ClueAuthoringResult.Rejected(it)
            }
            return ClueAuthoringResult.Accepted(
                ClueAuthority(
                    schemaVersion = CURRENT_SCHEMA_VERSION,
                    clueText = normalizedClue,
                    expectedAnswer = normalizedAnswer,
                    origin = ClueOrigin.MANUAL,
                    generatedProvenance = null,
                ),
            )
        }

        fun generated(
            clueText: String,
            expectedAnswer: String,
            provenance: GeneratedClueProvenance,
        ): ClueAuthoringResult {
            val normalizedClue = normalize(clueText)
            val normalizedAnswer = normalize(expectedAnswer)
            validationError(normalizedClue, normalizedAnswer)?.let {
                return ClueAuthoringResult.Rejected(it)
            }
            return ClueAuthoringResult.Accepted(
                ClueAuthority(
                    schemaVersion = CURRENT_SCHEMA_VERSION,
                    clueText = normalizedClue,
                    expectedAnswer = normalizedAnswer,
                    origin = ClueOrigin.GENERATED,
                    generatedProvenance = provenance,
                ),
            )
        }

        fun legacy(clueText: String): ClueAuthority = ClueAuthority(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            clueText = normalize(clueText),
            expectedAnswer = null,
            origin = ClueOrigin.LEGACY,
            generatedProvenance = null,
        )

        fun persisted(
            schemaVersion: Int,
            clueText: String,
            expectedAnswer: String?,
            origin: String,
            generatedProviderId: String?,
            generatedModelId: String?,
            generatedConfidence: Double?,
        ): ClueAuthority {
            val parsedOrigin = ClueOrigin.entries.firstOrNull { it.name == origin }
                ?: throw IllegalArgumentException("unsupported clue authority origin")
            val provenance = when {
                generatedProviderId == null && generatedModelId == null && generatedConfidence == null -> null
                generatedProviderId != null && generatedModelId != null -> GeneratedClueProvenance(
                    providerId = generatedProviderId,
                    modelId = generatedModelId,
                    confidence = generatedConfidence,
                )
                else -> throw IllegalArgumentException("incomplete generated clue provenance")
            }
            return ClueAuthority(
                schemaVersion = schemaVersion,
                clueText = normalize(clueText),
                expectedAnswer = expectedAnswer?.let(::normalize),
                origin = parsedOrigin,
                generatedProvenance = provenance,
            )
        }

        private fun validationError(clueText: String, expectedAnswer: String): ClueValidationError? = when {
            clueText.isBlank() -> ClueValidationError.BLANK_CLUE
            clueText.length > MAX_CLUE_LENGTH -> ClueValidationError.CLUE_TOO_LONG
            expectedAnswer.isBlank() -> ClueValidationError.BLANK_EXPECTED_ANSWER
            expectedAnswer.length > MAX_EXPECTED_ANSWER_LENGTH -> ClueValidationError.EXPECTED_ANSWER_TOO_LONG
            else -> null
        }

        private fun requireValidExpectedAnswer(expectedAnswer: String?) {
            require(expectedAnswer != null && expectedAnswer.isNotBlank()) { "expected answer must not be blank" }
            require(expectedAnswer.length <= MAX_EXPECTED_ANSWER_LENGTH) { "expected answer is too long" }
            require(expectedAnswer == normalize(expectedAnswer)) { "expected answer must be normalized" }
        }

        private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")
    }
}

data class ClueGenerationInput(
    val observation: String,
) {
    init {
        require(observation.isNotBlank()) { "observation must not be blank" }
        require(observation.length <= MAX_OBSERVATION_LENGTH) { "observation is too long" }
    }

    companion object {
        const val MAX_OBSERVATION_LENGTH: Int = 4096
    }
}

sealed interface ClueGenerationResult {
    data class Generated(val authority: ClueAuthority) : ClueGenerationResult {
        init {
            require(authority.origin == ClueOrigin.GENERATED) {
                "generated result must carry generated clue authority"
            }
        }
    }

    data class Unavailable(val diagnosticCode: String) : ClueGenerationResult
    data class Failed(val diagnosticCode: String) : ClueGenerationResult
}

/**
 * Optional closed-alpha clue generation boundary. Implementations used by the backendless core must execute
 * on-device; an unavailable or failed generator does not authorize remote inference or block manual authoring.
 */
interface ClueGenerator {
    val available: Boolean

    suspend fun generate(input: ClueGenerationInput): ClueGenerationResult
}
