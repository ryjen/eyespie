package com.micrantha.eyespie.platform.scan

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
        val controller = IosCameraCaptureController(successfulStore())

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
    fun captureWaitsForPreparationBeforePersisting() = runTest {
        val preparationStarted = CompletableDeferred<Unit>()
        val releasePreparation = CompletableDeferred<Unit>()
        val persisted = CompletableDeferred<Unit>()
        val store = object : IosCameraCaptureStoreContract {
            override suspend fun prepare(): Result<Unit> {
                preparationStarted.complete(Unit)
                releasePreparation.await()
                return Result.success(Unit)
            }

            override suspend fun persist(image: CameraImage): Result<Path> {
                persisted.complete(Unit)
                return Result.success("/capture.png".toPath())
            }
        }
        val controller = IosCameraCaptureController(store)
        controller.updateFrame(FakeCameraImage(byteArrayOf(1)))

        val preparation = async { controller.prepare() }
        preparationStarted.await()
        val capture = async { controller.capture() }
        runCurrent()

        assertFalse(persisted.isCompleted)
        releasePreparation.complete(Unit)

        assertTrue(preparation.await().isSuccess)
        assertEquals("/capture.png".toPath(), capture.await().getOrThrow())
        assertTrue(persisted.isCompleted)
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

    @Test
    fun sequentialCapturesUseTheLatestOwnedFrame() = runTest {
        val captured = mutableListOf<ByteArray>()
        var sequence = 0
        val store = object : IosCameraCaptureStoreContract {
            override suspend fun prepare() = Result.success(Unit)

            override suspend fun persist(image: CameraImage): Result<Path> {
                captured += image.toByteArray()
                sequence += 1
                return Result.success("/capture-$sequence.png".toPath())
            }
        }
        val controller = IosCameraCaptureController(store)

        controller.updateFrame(FakeCameraImage(byteArrayOf(1)))
        val first = controller.capture().getOrThrow()
        controller.updateFrame(FakeCameraImage(byteArrayOf(2)))
        val second = controller.capture().getOrThrow()

        assertEquals("/capture-1.png".toPath(), first)
        assertEquals("/capture-2.png".toPath(), second)
        assertContentEquals(byteArrayOf(1), captured[0])
        assertContentEquals(byteArrayOf(2), captured[1])
    }

    @Test
    fun failedCaptureReleasesTheInFlightGateForRetry() = runTest {
        var attempts = 0
        val store = object : IosCameraCaptureStoreContract {
            override suspend fun prepare() = Result.success(Unit)

            override suspend fun persist(image: CameraImage): Result<Path> {
                attempts += 1
                return if (attempts == 1) {
                    Result.failure(IosCameraCaptureException(IosCameraCaptureFailure.StorageFailed))
                } else {
                    Result.success("/capture.png".toPath())
                }
            }
        }
        val controller = IosCameraCaptureController(store)
        controller.updateFrame(FakeCameraImage(byteArrayOf(1)))

        val first = controller.capture()
        val second = controller.capture()

        assertCaptureFailure(first, IosCameraCaptureFailure.StorageFailed)
        assertEquals("/capture.png".toPath(), second.getOrThrow())
        assertEquals(2, attempts)
    }

    private fun successfulStore() = object : IosCameraCaptureStoreContract {
        override suspend fun prepare() = Result.success(Unit)
        override suspend fun persist(image: CameraImage) = Result.success("/capture.png".toPath())
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
