package com.micrantha.eyespie.domain.entities

import com.micrantha.eyespie.domain.ai.GeneratedClueProvenance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ClueTextLimits {
    const val MAX_CLUES = 3
    const val MAX_CLUE_LENGTH = 240
    const val MAX_ANSWER_LENGTH = 120
}

@Serializable
data class ClueAuthority(
    val entries: List<AuthoredClue>,
) {
    init {
        require(entries.size in 1..ClueTextLimits.MAX_CLUES) {
            "clue authority must contain between one and ${ClueTextLimits.MAX_CLUES} entries"
        }
    }

    fun guesserView(): GuessProof = entries
        .map { GuessClue(it.clue) }
        .toSet()
}

@Serializable
sealed interface AuthoredClue {
    val clue: String
    val expectedAnswer: String

    @Serializable
    @SerialName("generated")
    data class Generated(
        override val clue: String,
        override val expectedAnswer: String,
        val confidence: Float,
        val provenance: GeneratedClueProvenance,
    ) : AuthoredClue {
        init {
            requireValidClueFields(clue, expectedAnswer)
            require(confidence.isFinite() && confidence in 0f..1f) {
                "generated clue confidence must be finite and between zero and one"
            }
        }
    }

    @Serializable
    @SerialName("manual")
    data class Manual(
        override val clue: String,
        override val expectedAnswer: String,
    ) : AuthoredClue {
        init {
            requireValidClueFields(clue, expectedAnswer)
            require(clue == normalizeManualText(clue)) { "manual clue must be normalized" }
            require(expectedAnswer == normalizeManualText(expectedAnswer)) {
                "manual expected answer must be normalized"
            }
        }
    }
}

class InvalidManualClueException : IllegalArgumentException("manual clue input is invalid")

fun manualClue(
    clue: String,
    expectedAnswer: String,
): Result<AuthoredClue.Manual> {
    val normalizedClue = normalizeManualText(clue)
    val normalizedAnswer = normalizeManualText(expectedAnswer)
    return try {
        Result.success(AuthoredClue.Manual(normalizedClue, normalizedAnswer))
    } catch (_: IllegalArgumentException) {
        Result.failure(InvalidManualClueException())
    }
}

fun AiClue.toAuthoredGenerated(provenance: GeneratedClueProvenance) = AuthoredClue.Generated(
    clue = data.trim(),
    expectedAnswer = answer.trim(),
    confidence = confidence,
    provenance = provenance,
)

private fun requireValidClueFields(clue: String, expectedAnswer: String) {
    require(clue.isNotBlank() && clue.length <= ClueTextLimits.MAX_CLUE_LENGTH) {
        "clue text is invalid"
    }
    require(expectedAnswer.isNotBlank() && expectedAnswer.length <= ClueTextLimits.MAX_ANSWER_LENGTH) {
        "expected answer is invalid"
    }
}

private fun normalizeManualText(value: String): String = value
    .trim()
    .replace(Regex("\\s+"), " ")
