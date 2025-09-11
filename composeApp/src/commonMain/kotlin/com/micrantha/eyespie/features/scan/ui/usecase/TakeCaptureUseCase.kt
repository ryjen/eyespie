package com.micrantha.eyespie.features.scan.ui.usecase

import com.benasher44.uuid.uuid4
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.domain.usecase.dispatchUseCase
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import kotlin.coroutines.coroutineContext

class TakeCaptureUseCase(
    private val platform: Platform,
    private val dispatcher: Dispatcher,
) : Dispatcher by dispatcher {

    suspend operator fun invoke(image: CameraImage): Result<Path> = dispatchUseCase(coroutineContext) {
        withContext(Dispatchers.IO) {
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY.div(uuid4().toString()).apply {
                platform.fileWrite(this, image.toByteArray())
            }
        }
    }
}
