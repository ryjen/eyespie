package com.micrantha.eyespie.features.scan.data

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CaptureFileStoreTest {
    private val fileSystem = FakeFileSystem()
    private val store = OkioCaptureFileStore(fileSystem)
    private val capturePath = "/capture/eyespie-capture-test.jpg".toPath()

    @Test
    fun deletesExistingCapture() {
        fileSystem.createDirectories(capturePath.parent!!)
        fileSystem.write(capturePath) {
            writeUtf8("capture")
        }

        val result = store.delete(capturePath)

        assertTrue(result.isSuccess)
        assertNull(fileSystem.metadataOrNull(capturePath))
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun deletingMissingCaptureIsIdempotent() {
        val result = store.delete(capturePath)

        assertTrue(result.isSuccess)
        assertNull(fileSystem.metadataOrNull(capturePath))
        fileSystem.checkNoOpenFiles()
    }
}
