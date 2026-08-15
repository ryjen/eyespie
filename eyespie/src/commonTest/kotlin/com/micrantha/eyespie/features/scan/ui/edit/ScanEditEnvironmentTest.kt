package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.ui.geometry.Rect
import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.ai.GeneratedClueProvenance
import com.micrantha.eyespie.domain.ai.GeneratedClues
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.AnalyzedClues
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.entities.ScanEditState
import com.micrantha.eyespie.features.scan.usecase.FakeUploadCaptureUseCase
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
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScanEditEnvironmentTest {

    private val generatedClues = GeneratedClues(
        clues = setOf(AiClue("something red", 0.9f, "apple")),
        provenance = GeneratedClueProvenance(
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
        ),
    )
    private val clueRepository = object : ClueRepository {
        override suspend fun clues(image: Path): Result<GeneratedClues> = Result.success(generatedClues)
        override suspend fun guess(image: Path, clue: GuessClue): Result<String> = Result.success("")
    }
    private val loadCameraImageUseCase = object : LoadCameraImageUseCase {
        override fun invoke(path: Path, regionOfInterest: Rect?): Result<CameraImage> = Result.success(object : CameraImage {
            override val width = 0
            override val height = 0
            override fun toByteArray() = byteArrayOf()
            override fun toImageBitmap() = TODO()
        })
    }
    private val uploadCaptureUseCase = FakeUploadCaptureUseCase()
    private val dispatcher = FakeDispatcher()
    private val context = FakeScreenContext(dispatcher = dispatcher)
    private val environment = ScanEditEnvironment(context, uploadCaptureUseCase, clueRepository, loadCameraImageUseCase)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `analyzed clues should retain provenance while mapping selectable candidates`() {
        val state = environment.reduce(ScanEditState(), AnalyzedClues(generatedClues))

        assertSame(generatedClues, state.clues)
        assertEquals("test-local", state.clues?.provenance?.providerId)
        assertEquals("something red", state.selected?.values?.single()?.clue)
        assertEquals("apple", state.selected?.values?.single()?.answer)
    }

    @Test
    fun `invoke Save action should call upload use case and navigate back`() = runTest {
        val state = ScanEditState(
            path = "/test.jpg".toPath(),
            location = com.micrantha.eyespie.domain.entities.Location(
                point = com.micrantha.eyespie.domain.entities.Location.Point(1.0, 2.0)
            )
        )

        uploadCaptureUseCase.result = Result.success(com.micrantha.eyespie.domain.entities.Thing(
            id = "t1",
            createdBy = Player.Ref("p1", "p1"),
            imageUrl = "url",
            createdAt = Clock.System.now(),
            location = com.micrantha.eyespie.domain.entities.Location.Point(1.0, 2.0),
            guessed = false,
            guesses = emptyList()
        ))

        environment.invoke(SaveScanEdit, state)

        assertTrue(context.router.navigateBackCalled)
    }
}
