package com.micrantha.eyespie.platform.scan

import android.content.ContentValues
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.WindowManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import okio.Path
import okio.Path.Companion.toPath
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
actual fun CameraCapture(
    modifier: Modifier,
    regionOfInterest: Rect?,
    onCameraError: (Throwable) -> Unit,
    onCameraImage: (Path) -> Unit,
    captureButton: @Composable (() -> Unit) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val resolutionSelector = remember {
        ResolutionSelector.Builder()
            .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(400, 400),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            ).build()
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(context.getDisplayRotation())
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .build()
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    val outputOptions = remember {
        ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues()
        ).build()
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val rotation = remember { mutableIntStateOf(context.getDisplayRotation()) }

    DisposableEffect(Unit) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                rotation.intValue = context.getDisplayRotation()
                imageCapture.targetRotation = rotation.intValue
            }
        }
        displayManager.registerDisplayListener(listener, null)
        onDispose {
            displayManager.unregisterDisplayListener(listener)
        }
    }

    DisposableEffect(cameraSelector) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val context = previewView.context
            val executor = ContextCompat.getMainExecutor(context)

            ProcessCameraProvider.getInstance(context).apply {
                addListener({
                    cameraProvider = get().apply {
                        unbindAll()
                        camera = bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            createCameraUseCases(
                                previewView.surfaceProvider,
                                resolutionSelector,
                                imageCapture,
                            )
                        ).apply {
                            previewView.enableZoom(this)
                        }
                    }
                }, executor)
            }

        }
    )

    captureButton {
        val meteringPointFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        val centerPoint = meteringPointFactory.createPoint(0.5f, 0.5f)

        val focusAction = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()

        camera?.cameraControl?.startFocusAndMetering(focusAction)?.addListener({
            val executor = Executors.newSingleThreadExecutor()
            imageCapture.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        onCameraError(exception)
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        onCameraImage(context.saveImageToPath(outputFileResults.savedUri!!))
                    }
                }
            )
        }, ContextCompat.getMainExecutor(context))
    }
}

fun exifOrientationToDegrees(orientation: Int): Int {
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

private fun Context.getDisplayRotation(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display.rotation
    } else {
        @Suppress("DEPRECATION")
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.rotation
    }
}

fun Context.saveImageToPath(uri: Uri): Path {
    val destinationFile = File.createTempFile("camera", "capture", cacheDir)
    contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destinationFile).use { output ->
            input.copyTo(output)
        }
    }
    return destinationFile.absolutePath.toPath()
}


private fun createCameraUseCases(
    surfaceProvider: SurfaceProvider,
    resolutionSelector: ResolutionSelector,
    imageCapture: ImageCapture
): UseCaseGroup {

    val useCases = UseCaseGroup.Builder()
        .addUseCase(imageCapture)

    val previewUseCase = Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()
        .also { it.surfaceProvider = surfaceProvider }

    useCases.addUseCase(previewUseCase)

    val analysisUseCase = ImageAnalysis.Builder().setResolutionSelector(resolutionSelector).build()
    useCases.addUseCase(analysisUseCase)

    return useCases.build()
}

private fun PreviewView.enableZoom(camera: Camera) {

    val scaleGestureDetector =
        ScaleGestureDetector(context, object : SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                return camera.cameraInfo.zoomState.value?.let { zoom ->
                    val scale = zoom.zoomRatio * detector.scaleFactor
                    camera.cameraControl.setZoomRatio(scale)
                    true
                } ?: false
            }
        })

    setOnTouchListener { view, event ->
        view.performClick()
        scaleGestureDetector.onTouchEvent(event)
    }
}

private fun CameraSelector.toggle() = when (this) {
    CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
    else -> CameraSelector.DEFAULT_BACK_CAMERA
}
