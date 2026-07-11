package com.micrantha.eyespie.features.scan.data

import com.micrantha.bluebell.platform.ConnectivityStatus
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.features.scan.usecase.FakeUploadCaptureUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class, kotlin.time.ExperimentalTime::class)
class CaptureSyncRepositoryTest {

    private lateinit var source: FakeCaptureSyncSource
    private lateinit var uploadUseCase: FakeUploadCaptureUseCase
    private lateinit var networkMonitor: FakeNetworkMonitor
    private lateinit var connectivityStatus: ConnectivityStatus
    private lateinit var repository: CaptureSyncDataRepository
    private val testScope = TestScope()

    @BeforeTest
    fun setUp() {
        source = FakeCaptureSyncSource()
        uploadUseCase = FakeUploadCaptureUseCase()
        networkMonitor = FakeNetworkMonitor()
        connectivityStatus = ConnectivityStatus(networkMonitor)
        connectivityStatus.start()
        
        repository = CaptureSyncDataRepository(
            source,
            uploadUseCase,
            connectivityStatus,
            Json,
            testScope
        )
    }

    @Test
    fun `should queue capture when disconnected`() = runTest {
        networkMonitor.update(false)

        val proof = Proof(clues = setOf(AiClue("c1", 1f, "a1")), location = null)
        val imagePath = "/test.jpg".toPath()
        
        repository.queue(proof, imagePath, "p1")

        assertEquals(1, source.queued.size)
        assertEquals(0, uploadUseCase.invokedWith?.let { 1 } ?: 0)
    }

    @Test
    fun `should sync captures when connectivity is restored`() = runTest {
        networkMonitor.update(false)
        val proof = Proof(clues = setOf(AiClue("c1", 1f, "a1")), location = null)
        val imagePath = "/test.jpg".toPath()
        repository.queue(proof, imagePath, "p1")
        
        uploadUseCase.result = Result.success(com.micrantha.eyespie.domain.entities.Thing(
            id = "t1",
            createdBy = com.micrantha.eyespie.features.players.domain.entities.Player.Ref("p1", "p1"),
            imageUrl = "url",
            createdAt = kotlin.time.Clock.System.now(),
            location = com.micrantha.eyespie.domain.entities.Location.Point(0.0, 0.0),
            guessed = false,
            guesses = emptyList()
        ))

        networkMonitor.update(true)

        // Wait for sync to complete in the background scope
        testScope.testScheduler.runCurrent()

        assertEquals(0, source.queued.size)
        assertEquals(1, uploadUseCase.invokedWith?.let { 1 } ?: 0)
    }
}
