@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.micrantha.eyespie.imaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreGraphics.CGRect
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferRelease
import platform.CoreVideo.CVPixelBufferRetain
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.Foundation.NSError
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationRight
import platform.ImageIO.kCGImagePropertyOrientationUp
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.resume

@Composable
actual fun CameraCapture(
    modifier: Modifier,
    onCameraError: (Throwable) -> Unit,
    onCaptured: (CapturedImage) -> Unit,
    captureButton: @Composable ((capture: () -> Unit) -> Unit),
) {
    val controller = remember { IosCameraCaptureController() }
    val compositionScope = rememberCoroutineScope()
    var authorized by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        authorized = requestCameraAccess()
        if (authorized == false) {
            onCameraError(SecurityException("camera permission was denied"))
        }
    }

    val device = remember(authorized) {
        if (authorized == true) AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) else null
    }
    val stream = remember(device, onCameraError) {
        device?.let {
            CameraStream(
                device = it,
                onCameraError = onCameraError,
                onCameraFrame = controller::updateFrame,
            )
        }
    }

    if (authorized == true && stream == null) {
        LaunchedEffect(Unit) {
            onCameraError(IllegalStateException("no video capture device is available"))
        }
    }

    if (stream != null) {
        DisposableEffect(stream) {
            runCatching {
                stream.setup()
                stream.start()
            }.onFailure(onCameraError)

            onDispose { stream.stop() }
        }

        UIKitView(
            factory = stream::preview,
            onResize = stream::resize,
            modifier = modifier,
        )
    }

    captureButton {
        compositionScope.launch {
            if (authorized != true) {
                authorized = requestCameraAccess()
                if (authorized != true) {
                    onCameraError(SecurityException("camera permission was denied"))
                    return@launch
                }
            }
            controller.capture()
                .onSuccess(onCaptured)
                .onFailure(onCameraError)
        }
    }
}

private suspend fun requestCameraAccess(): Boolean =
    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> true
        AVAuthorizationStatusNotDetermined -> suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                if (continuation.isActive) continuation.resume(granted)
            }
        }
        else -> false
    }

private class IosCameraCaptureController {
    private val stateMutex = Mutex()
    private var latestFrame: OwnedCameraFrame? = null
    private var captureInFlight = false

    suspend fun updateFrame(frame: OwnedCameraFrame) {
        stateMutex.withLock { latestFrame = frame }
    }

    suspend fun capture(): Result<CapturedImage> {
        val selected = stateMutex.withLock {
            when {
                captureInFlight -> Result.failure(
                    IllegalStateException("a camera capture is already in progress"),
                )
                latestFrame == null -> Result.failure(
                    IllegalStateException("no camera frame is available to capture"),
                )
                else -> {
                    captureInFlight = true
                    Result.success(latestFrame!!)
                }
            }
        }
        val frame = selected.getOrElse { return Result.failure(it) }

        return try {
            Result.success(
                withContext(Dispatchers.Default) {
                    currentCoroutineContext().ensureActive()
                    frame.toCapturedImage()
                },
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Result.failure(error)
        } finally {
            withContext(NonCancellable) {
                stateMutex.withLock { captureInFlight = false }
            }
        }
    }
}

private class CameraStream(
    private val device: AVCaptureDevice,
    private val onCameraError: (Throwable) -> Unit,
    private val onCameraFrame: suspend (OwnedCameraFrame) -> Unit,
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    private val session = AVCaptureSession()
    private val workerJob = Job()
    private val scope = CoroutineScope(Dispatchers.Default) + workerJob
    private val dispatchQueue = dispatch_queue_create("eyespie.camera.frames", null)
    private var cameraPreviewLayer: AVCaptureVideoPreviewLayer? = null
    private var frameInFlight = false

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        if (frameInFlight) return
        val pixelBuffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: return
        val orientation = when (fromConnection.videoOrientation) {
            AVCaptureVideoOrientationPortraitUpsideDown -> kCGImagePropertyOrientationDown
            AVCaptureVideoOrientationLandscapeLeft -> kCGImagePropertyOrientationLeft
            AVCaptureVideoOrientationLandscapeRight -> kCGImagePropertyOrientationRight
            else -> kCGImagePropertyOrientationUp
        }

        CVPixelBufferRetain(pixelBuffer)
        frameInFlight = true
        scope.launch {
            try {
                val frame = try {
                    copyCameraFrame(pixelBuffer, orientation)
                } finally {
                    CVPixelBufferRelease(pixelBuffer)
                }
                onCameraFrame(frame)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onCameraError(error)
            } finally {
                dispatch_async(dispatchQueue) { frameInFlight = false }
            }
        }
    }

    @OptIn(BetaInteropApi::class)
    fun setup() {
        val input = memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            error.value = null
            val candidate = AVCaptureDeviceInput(device, error.ptr)
            error.value?.let {
                throw IllegalStateException("unable to create camera input (code=${it.code})")
            }
            candidate
        }
        check(session.canAddInput(input)) { "cannot add input to video session" }
        session.addInput(input)

        val output = AVCaptureVideoDataOutput().apply {
            setSampleBufferDelegate(this@CameraStream, dispatchQueue)
            if (availableVideoCVPixelFormatTypes.contains(
                    kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange,
                )
            ) {
                setVideoSettings(
                    mapOf(
                        kCVPixelBufferPixelFormatTypeKey to
                            kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange,
                    ),
                )
            }
        }
        check(session.canAddOutput(output)) { "cannot add output to video session" }
        session.addOutput(output)
    }

    fun start() {
        dispatch_async(dispatchQueue) { session.startRunning() }
    }

    fun stop() {
        scope.cancel()
        dispatch_async(dispatchQueue) { session.stopRunning() }
    }

    fun preview(): UIView {
        val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
            connection?.videoOrientation = AVCaptureVideoOrientationPortrait
        }
        cameraPreviewLayer = previewLayer
        return UIView().also { it.layer.addSublayer(previewLayer) }
    }

    fun resize(view: UIView, rect: CValue<CGRect>) {
        try {
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            view.layer.setFrame(rect)
            cameraPreviewLayer?.setFrame(rect)
        } finally {
            CATransaction.commit()
        }
    }
}
