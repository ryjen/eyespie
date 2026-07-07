package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.ui.geometry.Rect
import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.scan.data.FakeCaptureSyncRepository
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.entities.ScanEditState
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScanEditEnvironmentTest {

    private val captureSyncRepository = FakeCaptureSyncRepository()
    private val clueRepository = object : ClueRepository {
        override suspend fun clues(image: Path): Result<Set<AiClue>> = Result.success(emptySet())
        override suspend fun guess(image: Path, clue: GuessClue): Result<String> = Result.success("")
    }
    private val loadCameraImageUseCase = object : LoadCameraImageUseCase {
        override fun invoke(path: Path, regionOfInterest: Rect?): Result<CameraImage> = Result.failure(Exception())
    }
    private val dispatcher = FakeDispatcher()
    private val context = FakeScreenContext(dispatcher = dispatcher)
    private val environment = ScanEditEnvironment(context, captureSyncRepository, clueRepository, loadCameraImageUseCase)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invoke Save action should queue capture and navigate back`() = runTest {
        val state = ScanEditState(
            path = "/test.jpg".toPath(),
            location = com.micrantha.eyespie.domain.entities.Location(
                point = com.micrantha.eyespie.domain.entities.Location.Point(1.0, 2.0)
            )
        )
        
        CurrentSession.update(
            Player(
                "p1", 
                kotlinx.datetime.Instant.parse("2023-01-01T00:00:00Z"), 
                Player.Name("f", "l", "n"), 
                "e", 
                Player.Score(0)
            )
        )

        environment.invoke(SaveScanEdit, state)

        assertTrue(captureSyncRepository.queuedCalledWith != null)
        assertEquals(state.path, captureSyncRepository.queuedCalledWith?.second)
        assertTrue(context.router.navigateBackCalled)
    }
}
