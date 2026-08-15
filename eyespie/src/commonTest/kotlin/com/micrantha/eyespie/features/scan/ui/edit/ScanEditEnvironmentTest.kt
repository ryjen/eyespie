package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.ui.geometry.Rect
import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.ai.GeneratedClueProvenance
import com.micrantha.eyespie.domain.ai.GeneratedClues
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.AuthoredClue
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.scan.entities.ClueAuthoringMode
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.AnalyzedClues
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.ClueGenerationAvailability
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.GenerateClues
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.GeneratedCluesUnavailable
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveThingError
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SelectClue
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.UseManualAuthoring
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScanEditEnvironmentTest {

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
    private val generatedClues = GeneratedClues(
        clues = setOf(
            AiClue("clue A", 0.7f, "answer A"),
            AiClue("clue B", 0.8f, "answer B"),
            AiClue("clue C", 0.9f, "answer C"),
        ),
        provenance = provenance,
    )
    private val clueRepository = FakeClueRepository(generatedClues)
    private val cameraImage = object : CameraImage {
        override val width = 0
        override val height = 0
        override fun toByteArray() = byteArrayOf()
        override fun toImageBitmap() = TODO()
    }
    private val loadCameraImageUseCase = object : LoadCameraImageUseCase {
        override fun invoke(path: Path, regionOfInterest: Rect?): Result<CameraImage> =
            Result.success(cameraImage)
    }
    private val uploadCaptureUseCase = FakeUploadCaptureUseCase()
    private val dispatcher = FakeDispatcher()
    private val context = FakeScreenContext(dispatcher = dispatcher)
    private val environment = ScanEditEnvironment(
        context,
        uploadCaptureUseCase,
        clueRepository,
        loadCameraImageUseCase,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dispatcher.actions.clear()
        clueRepository.requests = 0
        clueRepository.generatedAvailable = true
        clueRepository.result = Result.success(generatedClues)
        uploadCaptureUseCase.invokedWith = null
        uploadCaptureUseCase.result = Result.success(savedThing())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loaded image should wait for explicit authoring choice without invoking inference`() = runTest {
        val loading = environment.reduce(
            ScanEditState(path = "/test.jpg".toPath()),
            cameraImage,
        )
<<<<<<< Updated upstream
||||||| Stash base
        
        uploadCaptureUseCase.result = Result.success(com.micrantha.eyespie.domain.entities.Thing(
            id = "t1",
            createdBy = Player.Ref("p1", "p1"),
            imageUrl = "url",
            createdAt = Clock.System.now(),
            location = com.micrantha.eyespie.domain.entities.Location.Point(1.0, 2.0),
            guessed = false,
            guesses = emptyList()
        ))
=======
        
        uploadCaptureUseCase.result = Result.success(com.micrantha.eyespie.domain.entities.Thing(
            id = "t1",
            createdBy = Player.Ref("p1", "p1"),
            imageUrl = "url",
            createdAt = Clock.System.now(),
            location = com.micrantha.eyespie.domain.entities.Location.Point(1.0, 2.0),
            clues = emptySet(),
            guessed = false,
            guesses = emptyList()
        ))
>>>>>>> Stashed changes

        assertTrue(loading.isBusy)
        environment.invoke(cameraImage, loading)
        val availability = assertIs<ClueGenerationAvailability>(dispatcher.actions.last())
        val state = environment.reduce(loading, availability)

        assertTrue(availability.available)
        assertEquals(ClueAuthoringMode.CHOOSE, state.authoringMode)
        assertFalse(state.isBusy)
        assertEquals(0, clueRepository.requests)
    }

    @Test
    fun `image generation unavailable should route directly to manual without provider request`() = runTest {
        clueRepository.generatedAvailable = false

        val loading = environment.reduce(
            ScanEditState(path = "/test.jpg".toPath()),
            cameraImage,
        )
        environment.invoke(cameraImage, loading)
        val availability = assertIs<ClueGenerationAvailability>(dispatcher.actions.last())
        val state = environment.reduce(loading, availability)

        assertFalse(availability.available)
        assertEquals(ClueAuthoringMode.MANUAL, state.authoringMode)
        assertTrue(state.generationUnavailable)
        assertFalse(state.isBusy)
        assertEquals(0, clueRepository.requests)
    }

    @Test
    fun `manual authoring can be selected without invoking generated provider`() = runTest {
        val state = environment.reduce(
            ScanEditState(path = "/test.jpg".toPath(), isBusy = false),
            UseManualAuthoring,
        )

        environment.invoke(UseManualAuthoring, state)

        assertEquals(ClueAuthoringMode.MANUAL, state.authoringMode)
        assertEquals(0, clueRepository.requests)
    }

    @Test
    fun `generated request rechecks readiness before invoking provider`() = runTest {
        clueRepository.generatedAvailable = false
        val generating = ScanEditState(
            path = "/test.jpg".toPath(),
            generationAvailable = true,
            authoringMode = ClueAuthoringMode.GENERATED,
            isBusy = true,
        )

        environment.invoke(GenerateClues, generating)

        assertEquals(0, clueRepository.requests)
        assertIs<GeneratedCluesUnavailable>(dispatcher.actions.last())
    }

    @Test
    fun `generated failure dispatches actionable manual fallback instead of generic load error`() = runTest {
        clueRepository.result = Result.failure(IllegalStateException("provider unavailable"))
        val generating = environment.reduce(
            ScanEditState(
                path = "/test.jpg".toPath(),
                generationAvailable = true,
                isBusy = false,
            ),
            GenerateClues,
        )

        environment.invoke(GenerateClues, generating)

        assertEquals(1, clueRepository.requests)
        val action = assertIs<GeneratedCluesUnavailable>(dispatcher.actions.last())
        val fallback = environment.reduce(generating, action)
        assertEquals(ClueAuthoringMode.MANUAL, fallback.authoringMode)
        assertFalse(fallback.generationAvailable)
        assertTrue(fallback.generationUnavailable)
        assertFalse(fallback.isBusy)
        assertFalse(fallback.isError)
    }

    @Test
    fun `analyzed clues retain provenance while mapping selectable candidates`() {
        val state = environment.reduce(ScanEditState(), AnalyzedClues(generatedClues))

        assertSame(generatedClues, state.clues)
        assertTrue(state.generationAvailable)
        assertEquals("test-local", state.clues?.provenance?.providerId)
        assertEquals(setOf("clue A", "clue B", "clue C"), state.selected?.values?.map { it.clue }?.toSet())
        assertEquals(setOf(0.7f, 0.8f, 0.9f), state.selected?.values?.map { it.confidence }?.toSet())
    }

    @Test
    fun `creator can switch to manual after generated candidates exist`() = runTest {
        val generated = environment.reduce(ScanEditState(), AnalyzedClues(generatedClues))
        val manual = environment.reduce(generated, UseManualAuthoring)

        environment.invoke(UseManualAuthoring, manual)

        assertEquals(ClueAuthoringMode.MANUAL, manual.authoringMode)
        assertSame(generatedClues, manual.clues)
        assertEquals(0, clueRepository.requests)
    }

    @Test
    fun `save persists only selected generated clue in local authority`() = runTest {
        val generated = environment.reduce(
            ScanEditState(
                path = "/test.jpg".toPath(),
                location = Location(point = Location.Point(1.0, 2.0)),
            ),
            AnalyzedClues(generatedClues),
        )
        val selected = environment.reduce(generated, SelectClue(1))

        environment.invoke(SaveScanEdit, selected)

        val authority = requireNotNull(uploadCaptureUseCase.invokedWith?.first?.clues)
        val entry = assertIs<AuthoredClue.Generated>(authority.entries.single())
        assertEquals("clue B", entry.clue)
        assertEquals("answer B", entry.expectedAnswer)
        assertEquals(0.8f, entry.confidence)
        assertEquals(provenance, entry.provenance)
        assertTrue(context.router.navigateBackCalled)
    }

    @Test
    fun `manual save normalizes input and records explicit manual origin`() = runTest {
        val state = ScanEditState(
            path = "/test.jpg".toPath(),
            location = Location(point = Location.Point(1.0, 2.0)),
            authoringMode = ClueAuthoringMode.MANUAL,
            manualClue = "  something   round and red ",
            manualAnswer = " red   apple ",
            isBusy = false,
        )

        assertTrue(environment.map(state).enabled)
        environment.invoke(SaveScanEdit, state)

        val authority = requireNotNull(uploadCaptureUseCase.invokedWith?.first?.clues)
        val entry = assertIs<AuthoredClue.Manual>(authority.entries.single())
        assertEquals("something round and red", entry.clue)
        assertEquals("red apple", entry.expectedAnswer)
    }

    @Test
    fun `invalid manual input cannot be submitted`() = runTest {
        val state = ScanEditState(
            path = "/test.jpg".toPath(),
            authoringMode = ClueAuthoringMode.MANUAL,
            manualClue = " ",
            manualAnswer = "apple",
            isBusy = false,
        )

        assertFalse(environment.map(state).enabled)
        environment.invoke(SaveScanEdit, state)

        assertEquals(null, uploadCaptureUseCase.invokedWith)
        assertIs<SaveThingError>(dispatcher.actions.last())
    }

    private fun savedThing() = Thing(
        id = "t1",
        createdBy = Player.Ref("p1", "p1"),
        imageUrl = "url",
        createdAt = Clock.System.now(),
        location = Location.Point(1.0, 2.0),
        guessed = false,
        guesses = emptyList(),
    )

    private class FakeClueRepository(
        generatedClues: GeneratedClues,
    ) : ClueRepository {
        var requests = 0
        var generatedAvailable = true
        var result: Result<GeneratedClues> = Result.success(generatedClues)

        override val canGenerateClues: Boolean
            get() = generatedAvailable

        override suspend fun clues(image: Path): Result<GeneratedClues> {
            requests += 1
            return result
        }

        override suspend fun guess(image: Path, clue: GuessClue): Result<String> = Result.success("")
    }
}
