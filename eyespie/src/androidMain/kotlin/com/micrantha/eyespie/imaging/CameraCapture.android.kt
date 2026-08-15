package com.micrantha.eyespie.imaging

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture as CameraXImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val CAPTURE_PREFIX = "eyespie-capture-"
private const val CAPTURE_SUFFIX = ".jpg"
private const val STALE_CAPTURE_AGE_MS = 24L * 60L * 60L * 1000L

// Process-scoped so a CameraX callback that completes after UI disposal still has an executor on
// which it can delete its app-private temporary file and release the capture gate.
private val cameraCaptureExecutor: ExecutorService by lazy {
    Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "eyespie-camera-capture").apply { isDaemon = true }
    }
}

@Composable
actual fun CameraCapture(
    modifier: Modifier,
    onCameraError: (Throwable) -> Unit,
    onCaptured: (CapturedImage) -> Unit,
    captureButton: @Composable ((capture: () -> Unit) -> Unit),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val compositionScope = rememberCoroutineScope()
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (!granted) {
            onCameraError(SecurityException("camera permission was denied"))
        }
    }

    // Do not trigger a native permission prompt merely because the surface entered composition.
    // The user-provided capture control is the explicit contextual request point.
    if (!permissionGranted) {
        captureButton { permissionLauncher.launch(Manifest.permission.CAMERA) }
        return
    }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val cameraXImageCapture = remember {
        CameraXImageCapture.Builder()
            .setCaptureMode(CameraXImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(CameraXImageCapture.FLASH_MODE_AUTO)
            .build()
    }
    val preview = remember(previewView) {
        Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
    }
    val imageCapture: ImageCapture = remember(context, cameraXImageCapture) {
        pruneStaleCaptureFiles(context.applicationContext)
        AndroidImageCapture(context.applicationContext, cameraXImageCapture)
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner, preview, cameraXImageCapture, imageCapture) {
        var disposed = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                runCatching {
                    val provider = future.get()
                    if (disposed) return@runCatching
                    cameraProvider = provider
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        cameraXImageCapture,
                    )
                }.onFailure {
                    if (!disposed) onCameraError(it)
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed = true
            cameraProvider?.unbind(preview, cameraXImageCapture)
            cameraProvider = null
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )

    captureButton {
        cameraXImageCapture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
        compositionScope.launch {
            try {
                onCaptured(imageCapture.capture())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onCameraError(error)
            }
        }
    }
}

/**
 * CameraX-backed implementation of the common [ImageCapture] contract.
 *
 * One capture may be owned at a time. A cancelled caller does not release that gate early because
 * CameraX may still be writing the app-private temporary file; the callback owns final cleanup.
 */
private class AndroidImageCapture(
    private val context: Context,
    private val cameraXImageCapture: CameraXImageCapture,
    private val executor: ExecutorService = cameraCaptureExecutor,
) : ImageCapture {
    private val captureInFlight = AtomicBoolean(false)

    override suspend fun capture(): CapturedImage {
        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        ) { "camera permission is unavailable" }
        check(captureInFlight.compareAndSet(false, true)) {
            "a camera capture is already in progress"
        }

        val outputFile = try {
            File.createTempFile(CAPTURE_PREFIX, CAPTURE_SUFFIX, context.cacheDir)
        } catch (error: Throwable) {
            captureInFlight.set(false)
            throw error
        }
        val outputOptions = CameraXImageCapture.OutputFileOptions.Builder(outputFile).build()

        return suspendCancellableCoroutine { continuation ->
            fun finish() {
                outputFile.delete()
                captureInFlight.set(false)
            }

            try {
                cameraXImageCapture.takePicture(
                    outputOptions,
                    executor,
                    object : CameraXImageCapture.OnImageSavedCallback {
                        override fun onError(exception: ImageCaptureException) {
                            finish()
                            if (continuation.isActive) {
                                continuation.resumeWithException(exception)
                            }
                        }

                        override fun onImageSaved(
                            outputFileResults: CameraXImageCapture.OutputFileResults,
                        ) {
                            try {
                                if (continuation.isActive) {
                                    continuation.resume(
                                        CapturedImage.fromEncoded(outputFile.readBytes()),
                                    )
                                }
                            } catch (error: Throwable) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(error)
                                }
                            } finally {
                                finish()
                            }
                        }
                    },
                )
            } catch (error: Throwable) {
                finish()
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }
        }
    }
}

private fun pruneStaleCaptureFiles(context: Context, nowMillis: Long = System.currentTimeMillis()) {
    val cutoff = nowMillis - STALE_CAPTURE_AGE_MS
    context.cacheDir.listFiles { file ->
        file.isFile &&
            file.name.startsWith(CAPTURE_PREFIX) &&
            file.name.endsWith(CAPTURE_SUFFIX) &&
            file.lastModified() < cutoff
    }?.forEach { stale ->
        runCatching { stale.delete() }
    }
}
