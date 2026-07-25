package com.micrantha.eyespie.features.onboarding.arch

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CapabilityRequestCoordinatorTest {
    @Test
    fun `duplicate request is ignored before its block starts`() = runTest {
        val coordinator = CapabilityRequestCoordinator()
        val requestStarted = CompletableDeferred<Unit>()
        val releaseRequest = CompletableDeferred<Unit>()
        var executionCount = 0

        val first = async {
            coordinator.runExclusive {
                executionCount += 1
                requestStarted.complete(Unit)
                releaseRequest.await()
                "first"
            }
        }
        requestStarted.await()

        val duplicate = coordinator.runExclusive {
            executionCount += 1
            "duplicate"
        }

        assertNull(duplicate)
        assertEquals(1, executionCount)

        releaseRequest.complete(Unit)
        assertEquals("first", first.await())
    }
}
