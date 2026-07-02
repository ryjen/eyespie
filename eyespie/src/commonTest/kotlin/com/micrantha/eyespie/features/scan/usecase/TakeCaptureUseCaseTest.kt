package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertTrue

class TakeCaptureUseCaseTest {

    private val platform = object : Platform {
        override val name = "Fake"
        override val networkMonitor = object : com.micrantha.bluebell.platform.NetworkMonitor {
            override fun startMonitoring(onUpdate: (Boolean) -> Unit) = Unit
            override fun stopMonitoring() = Unit
        }
        override fun format(epochSeconds: Long, format: String, timeZone: String) = ""
        override val locale: com.micrantha.bluebell.platform.Locale get() = TODO()
        override fun asset(path: Path) = TODO()
        override fun checksum(path: Path) = ""
        override fun resource(path: Path) = TODO()
        override fun format(format: String, vararg args: Any) = ""
        override fun filesPath() = "/".toPath()
        override fun sharedFilesPath() = "/".toPath()
        override fun fileWrite(path: Path, data: ByteArray) = Unit
        override fun fileRead(path: Path) = byteArrayOf()
    }
    private val dispatcher = FakeDispatcher()
    private val useCase = TakeCaptureUseCase(platform, dispatcher)

    @Test
    fun `invoke should return path on success`() = runTest {
        val image = object : CameraImage {
            override val width = 0
            override val height = 0
            override fun toByteArray() = byteArrayOf(1, 2, 3)
            override fun toImageBitmap() = TODO()
        }

        val result = useCase(image)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().toString().isNotEmpty())
    }
}
