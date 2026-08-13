package com.micrantha.eyespie.platform.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CameraAnalyzerTest {
    @Test
    fun proxyIsClosedBeforeAsyncAnalysisAndNewFramesAreDroppedWhileBusy() = runTest {
        val first = imageProxy()
        val second = imageProxy()
        val callbackStarted = CompletableDeferred<Unit>()
        val releaseCallback = CompletableDeferred<Unit>()
        var callbackCount = 0
        val errors = mutableListOf<Throwable>()

        val analyzer = CameraAnalyzer(
            callback = {
                callbackCount += 1
                verify(exactly = 1) { first.close() }
                callbackStarted.complete(Unit)
                releaseCallback.await()
            },
            errorCallback = errors::add,
            scope = this,
        )

        analyzer.analyze(first)
        runCurrent()
        callbackStarted.await()

        analyzer.analyze(second)

        verify(exactly = 1) { second.close() }
        verify(exactly = 0) { second.toBitmap() }
        assertEquals(1, callbackCount)
        assertTrue(errors.isEmpty())

        releaseCallback.complete(Unit)
        runCurrent()
    }

    @Test
    fun conversionFailureClosesProxyAndAllowsNextFrame() = runTest {
        val failed = imageProxy().also {
            every { it.toBitmap() } throws IllegalArgumentException("unsupported format")
        }
        val next = imageProxy()
        var callbackCount = 0
        val errors = mutableListOf<Throwable>()

        val analyzer = CameraAnalyzer(
            callback = { callbackCount += 1 },
            errorCallback = errors::add,
            scope = this,
        )

        analyzer.analyze(failed)
        analyzer.analyze(next)
        runCurrent()

        verify(exactly = 1) { failed.close() }
        verify(exactly = 1) { next.close() }
        verify(exactly = 1) { next.toBitmap() }
        assertEquals(1, callbackCount)
        assertEquals(1, errors.size)
    }

    private fun imageProxy(): ImageProxy {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val imageInfo = mockk<ImageInfo> {
            every { rotationDegrees } returns 90
            every { timestamp } returns 123L
        }
        return mockk(relaxed = true) {
            every { width } returns bitmap.width
            every { height } returns bitmap.height
            every { this@mockk.imageInfo } returns imageInfo
            every { toBitmap() } returns bitmap
        }
    }
}
