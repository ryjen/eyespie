package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Session
import com.micrantha.eyespie.domain.repository.FakeStorageRepository
import com.micrantha.eyespie.domain.repository.FakeThingRepository
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCase
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class UploadCaptureUseCaseTest {

    private val storageRepository = FakeStorageRepository()
    private val thingRepository = FakeThingRepository()
    private val fileSystem = object : FileSystem {
        override fun filesPath(): Path = "/".toPath()
        override fun sharedFilesPath(): Path = "/".toPath()
        override fun fileRead(path: Path): ByteArray = PNG_BYTES
        override fun fileWrite(path: Path, data: ByteArray) = Unit
    }
    private val session = CurrentSession
    private val imageEmbeddingGenerator = object : ImageEmbeddingGenerator {
        override suspend fun generate(image: CameraImage): Embedding = byteArrayOf(0, 0, 0, 0).toByteString()
    }
    private val loadCameraImageUseCase = object : LoadCameraImageUseCase {
        override fun invoke(path: Path, regionOfInterest: androidx.compose.ui.geometry.Rect?) = Result.success(object : CameraImage {
            override val width = 0
            override val height = 0
            override fun toByteArray() = byteArrayOf()
            override fun toImageBitmap() = TODO()
        })
    }

    private val useCase = UploadCaptureUseCaseImpl(
        storageRepository,
        thingRepository,
        fileSystem,
        imageEmbeddingGenerator,
        loadCameraImageUseCase,
        session
    )

    @Test
    fun `should persist opaque image object path and create thing`() = runTest {
        val player = Player("p1", Instant.parse("2023-01-01T00:00:00Z"), Player.Name("f", "l", "n"), "e", Player.Score(0))
        session.update(Session("a", "r", "u", "s"))
        session.update(player)

        val result = useCase(
            proof = Proof(clues = setOf(AiClue("clue", 0.9f, "answer")), location = null),
            image = "/test.png".toPath()
        )

        assertTrue(result.isSuccess)
        assertEquals(1, thingRepository.things.size)
        val imagePath = thingRepository.things.first().imagePath
        assertTrue(imagePath.matches(Regex("p1/.+\\.png")))
        assertFalse(imagePath.startsWith("http"))
        assertEquals(imagePath, storageRepository.storage.keys.single())
    }

    @Test
    fun `upload should fail closed when storage returns a different object identity`() = runTest {
        val player = Player("p1", Instant.parse("2023-01-01T00:00:00Z"), Player.Name("f", "l", "n"), "e", Player.Score(0))
        session.update(Session("a", "r", "u", "s"))
        session.update(player)
        storageRepository.uploadResult = Result.success("other-player/unexpected.png")

        val result = useCase(
            proof = Proof(clues = null, location = null),
            image = "/test.png".toPath(),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `capture format detection supports PNG and JPEG`() {
        assertEquals("png", PNG_BYTES.captureImageExtension())
        assertEquals("jpg", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00).captureImageExtension())
    }

    @Test
    fun `capture format detection rejects unknown bytes`() {
        assertFailsWith<IllegalArgumentException> {
            byteArrayOf(1, 2, 3).captureImageExtension()
        }
    }

    private companion object {
        val PNG_BYTES = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3,
        )
    }
}
