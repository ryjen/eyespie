package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.bluebell.domain.usecase.dispatchUseCase
import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.bluebell.platform.toImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path

class LoadImageUseCase(
    private val fileSystem: FileSystem
) {

    suspend operator fun invoke(path: Path) = dispatchUseCase(
        Dispatchers.IO
    ) {
        fileSystem.fileRead(path).toImageBitmap()
    }
}
