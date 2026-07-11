package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.ui.geometry.Rect
import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.domain.repository.FakeStorageRepository
import com.micrantha.eyespie.domain.repository.FakeThingRepository
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.scan.data.FakeCaptureSyncRepository
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.entities.ScanEditState
import com.micrantha.eyespie.features.scan.usecase.ImageEmbeddingGenerator
import com.micrantha.eyespie.features.scan.usecase.FakeUploadCaptureUseCase
import com.micrantha.eyespie.features.scan.usecase.UploadCaptureUseCase
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlin.time.Instant
import okio.ByteString.Companion.toByteString
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
