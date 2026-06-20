package com.micrantha.eyespie.data.clue

import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LabelDataRepositoryTest {

    private fun parseProof(output: String): Set<Pair<String, String>> =
        output.lines().chunked(3).map { (clue, answer, confidence) ->
            clue.trim() to answer.trim()
        }.toSet()

    @Test
    fun clues_prompt_contains_required_instructions() {
        val prompt = CluePromptSource().clues()
        assertTrue(
            actual = prompt.contains("three", ignoreCase = true),
            message = "Prompt should reference generating multiple clues",
        )
        assertTrue(
            actual = prompt.contains("confidence", ignoreCase = true),
            message = "Prompt should require a confidence score",
        )
    }

    @Test
    fun clues_prompt_is_stable() {
        val prompt = CluePromptSource().clues()
        assertEquals(
            expected = CluePromptSource().clues(),
            actual = prompt,
            message = "Clue prompt should not change unexpectedly",
        )
    }

    @Test
    fun guess_prompt_includes_clue_and_single_answer_format() {
        val prompt = CluePromptSource().guess("something round and red")
        assertTrue(
            actual = prompt.contains("something round and red"),
            message = "Guess prompt should interpolate the clue",
        )
        assertTrue(
            actual = prompt.contains("single word", ignoreCase = true),
            message = "Guess prompt should enforce terse answer format",
        )
    }

    @Test
    fun proof_parser_handles_valid_single_clue_output() {
        val output = """
            I spy something round and red.
            Apple
            0.95
        """.trimIndent()

        val parsed = parseProof(output)
        assertEquals(
            expected = 1,
            actual = parsed.size,
            message = "Single clue should parse to one entry",
        )
        assertTrue(
            actual = parsed.any { (clue, answer) ->
                clue.contains("round", ignoreCase = true) &&
                answer.equals("Apple", ignoreCase = true)
            },
            message = "Parsed clue and answer should match input triple",
        )
    }

    @Test
    fun proof_parser_deduplicates_text_trailing_spaces() {
        val output = """
            I spy something round and red.
            Apple
            0.95
        """.trimIndent()

        val parsed = parseProof(output)
        assertEquals(
            expected = 1,
            actual = parsed.size,
            message = "Trailing newlines should not create empty entries",
        )
    }

    @Test
    fun proof_parser_handles_multiple_clues() {
        val output = listOf(
            "I spy something round and red." to "Apple",
            "I spy something with leaves." to "Tree",
        ).flatMap { (clue, answer) ->
            listOf(clue, answer, "0.9")
        }.joinToString(separator = "\n")

        val parsed = parseProof(output)
        assertEquals(
            expected = 2,
            actual = parsed.size,
            message = "Two clues should parse to two entries",
        )
    }
}
