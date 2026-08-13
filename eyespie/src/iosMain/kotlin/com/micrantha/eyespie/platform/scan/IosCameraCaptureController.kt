package com.micrantha.eyespie.platform.scan

import com.benasher44.uuid.uuid4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

internal enum class IosCameraCaptureFailure(val diagnosticCode: String) {
    FrameUnavailable("camera.capture.frame_unavailable"),
    CaptureInProgress("camera.capture.in_progress"),
    EncodingFailed("camera.capture.encoding_failed"),
    StorageFailed("camera.capture.storage_failed"),
}

internal class IosCameraCaptureException(
    val failure: IosCameraCaptureFailure,
    cause: Throwable? = null,
) : IllegalStateException(
    when (failure) {
        IosCameraCaptureFailure.FrameUnavailable -> "no camera frame is available to capture"
        IosCameraCaptureFailure.CaptureInProgress -> "a camera capture is already in progress"
        IosCameraCaptureFailure.EncodingFailed -> "unable to encode camera capture"
        IosCameraCaptureFailure.StorageFailed -> "unable to persist camera capture"
    },
    cause,
)

internal interface IosCameraCaptureStoreContract {
    suspend fun prepare(): Result<Unit>
    suspend fun persist(image: CameraImage): Result<Path>
}

/**
 * Owns the temporary-file portion of the iOS still-capture contract.
 *
 * Captures live in a dedicated system-temporary directory. Stale captures from a prior capture
 * surface are pruned when a new surface is prepared. Once [persist] returns, ownership of the
 * resulting path transfers to the downstream ScanEdit flow; the current composable must not
 * delete that active file when it is disposed during navigation.
 */
internal class IosCameraCaptureStore(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val directory: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "eyespie-camera-captures",
    private val fileName: () -> String = { "${uuid4()}.png" },
) : IosCameraCaptureStoreContract {

    override suspend fun prepare(): Result<Unit> = runCatching {
        withContext(Dispatchers.Default) {
            try {
                fileSystem.createDirectories(directory)
                fileSystem.list(directory).forEach { stale ->
                    fileSystem.delete(stale, mustExist = false)
                }
            } catch (error: Throwable) {
                throw IosCameraCaptureException(IosCameraCaptureFailure.StorageFailed, error)
            }
        }
    }

    override suspend fun persist(image: CameraImage): Result<Path> = runCatching {
        withContext(Dispatchers.Default) {
            val encoded = try {
                image.toByteArray()
            } catch (error: Throwable) {
                throw IosCameraCaptureException(IosCameraCaptureFailure.EncodingFailed, error)
            }

            val path = directory / fileName()
            try {
                fileSystem.createDirectories(directory)
                fileSystem.write(path) {
                    write(encoded)
                }
                path
            } catch (error: Throwable) {
                runCatching { fileSystem.delete(path, mustExist = false) }
                throw IosCameraCaptureException(IosCameraCaptureFailure.StorageFailed, error)
            }
        }
    }
}

/**
 * Keeps only the most recent app-owned frame and serializes explicit capture actions.
 *
 * [CameraScanner] has already copied and oriented the frame before [updateFrame] is invoked, so
 * this controller never retains a borrowed CoreMedia/CoreVideo object.
 */
internal class IosCameraCaptureController(
    private val store: IosCameraCaptureStoreContract = IosCameraCaptureStore(),
) {
    private val mutex = Mutex()
    private var latestImage: CameraImage? = null
    private var captureInFlight = false

    suspend fun prepare(): Result<Unit> = store.prepare()

    suspend fun updateFrame(image: CameraImage) {
        mutex.withLock {
            latestImage = image
        }
    }

    suspend fun capture(): Result<Path> {
        val selection = mutex.withLock {
            when {
                captureInFlight -> Result.failure(
                    IosCameraCaptureException(IosCameraCaptureFailure.CaptureInProgress)
                )

                latestImage == null -> Result.failure(
                    IosCameraCaptureException(IosCameraCaptureFailure.FrameUnavailable)
                )

                else -> {
                    captureInFlight = true
                    Result.success(latestImage!!)
                }
            }
        }

        val image = selection.getOrElse { return Result.failure(it) }
        return try {
            store.persist(image)
        } finally {
            mutex.withLock {
                captureInFlight = false
            }
        }
    }
}
