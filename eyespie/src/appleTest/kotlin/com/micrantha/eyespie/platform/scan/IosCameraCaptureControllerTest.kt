package com.micrantha.eyespie.platform.scan

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IosCameraCaptureControllerTest {

    @Test
    fun captureBeforeFirstFrameFailsDeterministically() = runTest {
        val controller = IosCameraCaptureController(
            store = object : IosCameraCaptureStoreContract {
                override suspend fun prepare() = Result.success(Unit)
                override suspend fun persist(image: CameraImage) = Result.success("/capture.png".toPath())
            }
        )

        val result = controller.capture()

        assertCaptureFailure(result, IosCameraCaptureFailure.FrameUnavailable)
    }

    @Test
    fun ownedFramePersistsToDedicatedTemporaryStorage() = runTest {
        val fileSystem = FakeFileSystem()
        val directory = "/tmp/eyespie-camera-captures".toPath()
        val store = IosCameraCaptureStore(
            fileSystem = fileSystem,
            directory = directory,
            fileName = { "capture.png" },
        )
        val controller = IosCameraCaptureController(store)
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3)

        assertTrue(controller.prepare().isSuccess)
        controller.updateFrame(FakeCameraImage(bytes))
        val path = controller.capture().getOrThrow()

        assertEquals(directory / "capture.png", path)
        assertTrue(fileSystem.exists(path))
        assertContentEquals(bytes, fileSystem.read(path) { readByteArray() })
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun preparingNewCaptureSurfacePrunesStaleTemporaryCaptures() = runTest {
        val fileSystem = FakeFileSystem()
        val directory = "/tmp/eyespie-camera-captures".toPath()
        fileSystem.createDirectories(directory)
        val stale = directory / "stale.png"
        fileSystem.write(stale) { writeUtf8("stale") }
        val store = IosCameraCaptureStore(fileSystem, directory) { "fresh.png" }

        val result = store.prepare()

        assertTrue(result.isSuccess)
        assertFalse(fileSystem.exists(stale))
        assertTrue(fileSystem.exists(directory))
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun encodingFailureDoesNotCreatePartialCapture() = runTest {
        val fileSystem = FakeFileSystem()
        val directory = "/tmp/eyespie-camera-captures".toPath()
        val store = IosCameraCaptureStore(fileSystem, directory) { "capture.png" }
        val image = object : CameraImage {
            override val width = 1
            override val height = 1
            override fun toByteArray(): ByteArray = error("encode failed")
            override fun toImageBitmap(): ImageBitmap = error("not used")
        }

        val result = store.persist(image)

        assertCaptureFailure(result, IosCameraCaptureFailure.EncodingFailed)
        assertFalse(fileSystem.exists(directory / "capture.png"))
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun storageFailureRemovesPartialCapture() = runTest {
        val fileSystem = FakeFileSystem()
        val directory = "/blocked".toPath()
        fileSystem.write(directory) { writeUtf8("not a directory") }
        val store = IosCameraCaptureStore(fileSystem, directory) { "capture.png" }

        val result = store.persist(FakeCameraImage(byteArrayOf(1, 2, 3)))

        assertCaptureFailure(result, IosCameraCaptureFailure.StorageFailed)
        assertFalse(fileSystem.exists(directory / "capture.png"))
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun concurrentCaptureIsRejectedWhileFirstCaptureCompletes() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val store = object : IosCameraCaptureStoreContract {
            override suspend fun prepare() = Result.success(Unit)

            override suspend fun persist(image: CameraImage): Result<Path> {
                started.complete(Unit)
                release.await()
                return Result.success("/capture.png".toPath())
            }
        }
        val controller = IosCameraCaptureController(store)
        controller.updateFrame(FakeCameraImage(byteArrayOf(1)))

        val first = async { controller.capture() }
        started.await()
        val second = controller.capture()
        release.complete(Unit)

        assertCaptureFailure(second, IosCameraCaptureFailure.CaptureInProgress)
        assertEquals("/capture.png".toPath(), first.await().getOrThrow())
    }

    private fun assertCaptureFailure(result: Result<*>, failure: IosCameraCaptureFailure) {
        val error = assertIs<IosCameraCaptureException>(result.exceptionOrNull())
        assertEquals(failure, error.failure)
    }

    private class FakeCameraImage(
        private val bytes: ByteArray,
    ) : CameraImage {
        override val width = 1
        override val height = 1
        override fun toByteArray(): ByteArray = bytes
        override fun toImageBitmap(): ImageBitmap = error("not used")
    }
}
