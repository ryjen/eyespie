package com.micrantha.eyespie.features.scan.data

import okio.FileSystem
import okio.Path

interface CaptureFileStore {
    fun delete(path: Path): Result<Unit>
}

internal class OkioCaptureFileStore(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : CaptureFileStore {
    override fun delete(path: Path): Result<Unit> = runCatching {
        if (fileSystem.metadataOrNull(path) != null) {
            fileSystem.delete(path)
        }
    }
}
