package com.micrantha.eyespie.domain.entities

import com.micrantha.eyespie.domain.ai.GeneratedClueProvenance
import com.micrantha.eyespie.domain.ai.InferenceLocality
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClueAuthorityTest {

    private val provenance = GeneratedClueProvenance(
        schemaVersion = 1,
        providerId = "test-local",
        runtimeId = "test-runtime",
        locality = InferenceLocality.LOCAL,
        modelId = "test-model",
        modelVersion = "1",
        promptId = "eyespie-clue-generation",
        promptVersion = 1,
        executionConfiguration = null,
        repaired = false,
    )

    @Test
    fun `manual clue normalizes whitespace deterministically`() {
        val authored = manualClue(
            clue = "  something   round\nand red  ",
            expectedAnswer = "  red   apple ",
        ).getOrThrow()

        assertEquals("something round and red", authored.clue)
        assertEquals("red apple", authored.expectedAnswer)
    }

    @Test
    fun `manual clue rejects blank and oversized fields`() {
        assertTrue(manualClue("", "apple").isFailure)
        assertTrue(manualClue("red thing", " ").isFailure)
        assertTrue(
            manualClue("x".repeat(ClueTextLimits.MAX_CLUE_LENGTH + 1), "apple").isFailure
        )
        assertTrue(
            manualClue("red thing", "x".repeat(ClueTextLimits.MAX_ANSWER_LENGTH + 1)).isFailure
        )
    }

    @Test
    fun `manual authority contains no invented model metadata`() {
        val manual = manualClue("something red", "apple").getOrThrow()
        val encoded = Json.encodeToString(ClueAuthority(listOf(manual)))

        assertIs<AuthoredClue.Manual>(manual)
        assertFalse(encoded.contains("confidence"))
        assertFalse(encoded.contains("providerId"))
        assertFalse(encoded.contains("modelId"))
    }

    @Test
    fun `guesser view contains clue text without expected answer or provenance`() {
        val generated = AiClue(
            data = "something red",
            confidence = 0.91f,
            answer = "apple",
        ).toAuthoredGenerated(provenance)
        val authority = ClueAuthority(listOf(generated))

        val encoded = Json.encodeToString(authority.guesserView())

        assertEquals(setOf(GuessClue("something red")), authority.guesserView())
        assertFalse(encoded.contains("apple"))
        assertFalse(encoded.contains("answer"))
        assertFalse(encoded.contains("provider"))
    }

    @Test
    fun `generated authority preserves confidence and application provenance`() {
        val generated = AiClue(
            data = "something red",
            confidence = 0.91f,
            answer = "apple",
        ).toAuthoredGenerated(provenance)

        assertEquals("something red", generated.clue)
        assertEquals("apple", generated.expectedAnswer)
        assertEquals(0.91f, generated.confidence)
        assertEquals(provenance, generated.provenance)
    }
}
