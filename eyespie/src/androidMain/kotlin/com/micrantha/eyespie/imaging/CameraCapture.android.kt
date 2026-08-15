package com.micrantha.eyespie.imaging

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

private const val CAPTURE_PREFIX = "eyespie-capture-"
private const val CAPTURE_SUFFIX = ".jpg"

@Composable
actual fun CameraCapture(
    modifier: Modifier,
    onCameraError: (Throwable) -> Unit,
    onCaptured: (CapturedImage) -> Unit,
    captureButton: @Composable ((capture: () -> Unit) -> Unit),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!permissionGranted) {
        captureButton { permissionLauncher.launch(Manifest.permission.CAMERA) }
        return
    }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .build()
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner, previewView) {
        var disposed = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                runCatching {
                    val provider = future.get()
                    if (disposed) {
                        provider.unbindAll()
                        return@runCatching
                    }
                    cameraProvider = provider
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    provider.unbindAll()
                    if (disposed) return@runCatching
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                }.onFailure {
                    if (!disposed) onCameraError(it)
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed = true
            cameraProvider?.unbindAll()
            cameraProvider = null
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )

    captureButton {
        imageCapture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
        captureImage(context, imageCapture, onCameraError, onCaptured)
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    onCameraError: (Throwable) -> Unit,
    onCaptured: (CapturedImage) -> Unit,
) {
    val outputFile = runCatching {
        File.createTempFile(CAPTURE_PREFIX, CAPTURE_SUFFIX, context.cacheDir)
    }.getOrElse {
        onCameraError(it)
        return
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                outputFile.delete()
                onCameraError(exception)
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    onCaptured(CapturedImage.fromEncoded(outputFile.readBytes()))
                } catch (error: Throwable) {
                    onCameraError(error)
                } finally {
                    outputFile.delete()
                }
            }
        },
    )
}
