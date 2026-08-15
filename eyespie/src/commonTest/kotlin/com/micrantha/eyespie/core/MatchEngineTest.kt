package com.micrantha.eyespie.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatchEngineTest {
    private val engine = MatchEngine(threshold = 0.75)

    @Test
    fun identicalEmbeddingsMatch() {
        val result = engine.compare(
            target = listOf(1f, 2f, 3f),
            guess = listOf(1f, 2f, 3f),
        )

        assertEquals(1.0, result.similarity, absoluteTolerance = 1e-9)
        assertTrue(result.matched)
    }

    @Test
    fun orthogonalEmbeddingsDoNotMatch() {
        val result = engine.compare(
            target = listOf(1f, 0f),
            guess = listOf(0f, 1f),
        )

        assertEquals(0.0, result.similarity, absoluteTolerance = 1e-9)
        assertFalse(result.matched)
    }
}
