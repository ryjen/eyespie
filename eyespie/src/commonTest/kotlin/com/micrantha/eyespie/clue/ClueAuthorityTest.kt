package com.micrantha.eyespie.clue

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ClueAuthorityTest {
    @Test
    fun manualAuthoringNormalizesWhitespaceAndKeepsAnswerOutOfPlayableProjection() {
        val authority = accepted(
            ClueAuthority.manual(
                clueText = "  Something   with stripes  ",
                expectedAnswer = "  pedestrian   crossing ",
            ),
        )

        assertEquals("Something with stripes", authority.clueText)
        assertEquals("pedestrian crossing", authority.expectedAnswer)
        assertEquals(ClueOrigin.MANUAL, authority.origin)
        assertNull(authority.generatedProvenance)
        assertEquals(PlayableClue("Something with stripes"), authority.playable())
    }

    @Test
    fun manualAuthoringRejectsBlankAndOversizedFieldsDeterministically() {
        assertEquals(
            ClueAuthoringResult.Rejected(ClueValidationError.BLANK_CLUE),
            ClueAuthority.manual("   ", "answer"),
        )
        assertEquals(
            ClueAuthoringResult.Rejected(ClueValidationError.CLUE_TOO_LONG),
            ClueAuthority.manual("x".repeat(ClueAuthority.MAX_CLUE_LENGTH + 1), "answer"),
        )
        assertEquals(
            ClueAuthoringResult.Rejected(ClueValidationError.BLANK_EXPECTED_ANSWER),
            ClueAuthority.manual("clue", "   "),
        )
        assertEquals(
            ClueAuthoringResult.Rejected(ClueValidationError.EXPECTED_ANSWER_TOO_LONG),
            ClueAuthority.manual("clue", "x".repeat(ClueAuthority.MAX_EXPECTED_ANSWER_LENGTH + 1)),
        )
    }

    @Test
    fun generatedAuthorityRequiresTruthfulGeneratedProvenance() {
        val provenance = GeneratedClueProvenance(
            providerId = "local-provider",
            modelId = "model-v1",
            confidence = 0.8,
        )
        val authority = accepted(
            ClueAuthority.generated(
                clueText = "Find the striped crossing",
                expectedAnswer = "crosswalk",
                provenance = provenance,
            ),
        )

        assertEquals(ClueOrigin.GENERATED, authority.origin)
        assertEquals(provenance, authority.generatedProvenance)
    }

    @Test
    fun persistedManualAuthorityRejectsInjectedGeneratedProvenance() {
        assertFailsWith<IllegalArgumentException> {
            ClueAuthority.persisted(
                schemaVersion = ClueAuthority.CURRENT_SCHEMA_VERSION,
                clueText = "A clue",
                expectedAnswer = "answer",
                origin = ClueOrigin.MANUAL.name,
                generatedProviderId = "provider",
                generatedModelId = "model",
                generatedConfidence = null,
            )
        }
    }

    @Test
    fun legacyAuthorityPreservesOldPlayableClueWithoutInventingAnswer() {
        val authority = ClueAuthority.legacy("  Existing   clue ")

        assertEquals(ClueOrigin.LEGACY, authority.origin)
        assertEquals("Existing clue", authority.clueText)
        assertNull(authority.expectedAnswer)
        assertNull(authority.generatedProvenance)
    }

    private fun accepted(result: ClueAuthoringResult): ClueAuthority = when (result) {
        is ClueAuthoringResult.Accepted -> result.authority
        is ClueAuthoringResult.Rejected -> error("expected accepted clue authority, got ${result.error}")
    }
}
