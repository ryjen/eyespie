package com.micrantha.eyespie.platform.scan

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class IosCameraCaptureCancellationTest {

    @Test
    fun cancelledCaptureReleasesInFlightGateForRetry() = runTest {
        var attempts = 0
        val started = CompletableDeferred<Unit>()
        val store = object : IosCameraCaptureStoreContract {
            override suspend fun prepare() = Result.success(Unit)

            override suspend fun persist(image: CameraImage): Result<Path> {
                attempts += 1
                return if (attempts == 1) {
                    started.complete(Unit)
                    awaitCancellation()
                } else {
                    Result.success("/capture.png".toPath())
                }
            }
        }
        val controller = IosCameraCaptureController(store)
        controller.updateFrame(FakeCameraImage())

        val first = async { controller.capture() }
        started.await()
        first.cancelAndJoin()

        val second = controller.capture()

        assertEquals("/capture.png".toPath(), second.getOrThrow())
        assertEquals(2, attempts)
    }

    private class FakeCameraImage : CameraImage {
        override val width = 1
        override val height = 1
        override fun toByteArray(): ByteArray = byteArrayOf(1)
        override fun toImageBitmap(): ImageBitmap = error("not used")
    }
}
