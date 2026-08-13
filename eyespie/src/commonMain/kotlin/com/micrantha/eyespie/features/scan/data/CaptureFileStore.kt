package com.micrantha.eyespie.features.scan.data

import okio.FileSystem
import okio.Path

interface CaptureFileStore {
    fun delete(path: Path): Result<Unit>
}

internal class OkioCaptureFileStore : CaptureFileStore {
    override fun delete(path: Path): Result<Unit> = runCatching {
        if (FileSystem.SYSTEM.metadataOrNull(path) != null) {
            FileSystem.SYSTEM.delete(path)
        }
    }
}
