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
import platform.AVFoundation.*
import platform.CoreGraphics.CGRect
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferRelease
import platform.CoreVideo.CVPixelBufferRetain
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelBufferPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationRight
import platform.ImageIO.kCGImagePropertyOrientationUp
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIApplicationDidBecomeActiveNotification
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
    val controller: ImageCapture = remember { IosCameraCaptureController() }
    val compositionScope = rememberCoroutineScope()
    var authorized by remember { mutableStateOf(currentCameraAuthorization()) }

    // Reconcile OS-authoritative camera state after returning from Settings. This notification is
    // observation-only: it must never trigger a permission request merely because the app became
    // active. A revoked grant tears down the stream by recomposition; a new grant starts it again.
    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            null,
            null,
        ) {
            authorized = currentCameraAuthorization()
        }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }

    val device = remember(authorized) {
        if (authorized == true) AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) else null
    }
    val stream = remember(device, onCameraError) {
        device?.let {
            CameraStream(
                device = it,
                onCameraError = { error ->
                    compositionScope.launch { onCameraError(error) }
                },
                onCameraFrame = (controller as IosCameraCaptureController)::updateFrame,
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
                    onCameraError(IllegalStateException("camera permission was denied"))
                }
                // A newly granted permission must first recompose and start the camera session.
                return@launch
            }

            try {
                onCaptured(controller.capture())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onCameraError(error)
            }
        }
    }
}

private fun currentCameraAuthorization(): Boolean? =
    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> true
        AVAuthorizationStatusNotDetermined -> null
        else -> false
    }

private suspend fun requestCameraAccess(): Boolean =
    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> true
        AVAuthorizationStatusNotDetermined -> suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(
                mediaType = AVMediaTypeVideo,
            ) { granted: Boolean ->
                if (continuation.isActive) continuation.resume(granted)
            }
        }
        else -> false
    }

/**
 * iOS implementation of the common [ImageCapture] contract backed by the most recent app-owned
 * frame. At most one explicit encoding operation is owned at a time; concurrent taps fail closed.
 */
private class IosCameraCaptureController : ImageCapture {
    private val stateMutex = Mutex()
    private var latestFrame: OwnedCameraFrame? = null
    private var captureInFlight = false

    suspend fun updateFrame(frame: OwnedCameraFrame) {
        stateMutex.withLock { latestFrame = frame }
    }

    override suspend fun capture(): CapturedImage {
        val frame = stateMutex.withLock {
            when {
                captureInFlight -> throw IllegalStateException("a camera capture is already in progress")
                latestFrame == null -> throw IllegalStateException("no camera frame is available to capture")
                else -> {
                    captureInFlight = true
                    latestFrame!!
                }
            }
        }

        return try {
            withContext(Dispatchers.Default) {
                currentCoroutineContext().ensureActive()
                frame.toCapturedImage()
            }
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
        val conversionJob = scope.launch {
            try {
                val frame = copyCameraFrame(pixelBuffer, orientation)
                onCameraFrame(frame)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onCameraError(error)
            }
        }
        // Completion is the native ownership boundary. This also runs when the scope was already
        // cancelled before the coroutine body started, so every retain has exactly one release.
        conversionJob.invokeOnCompletion {
            CVPixelBufferRelease(pixelBuffer)
            dispatch_async(dispatchQueue) { frameInFlight = false }
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
                    kCVPixelBufferPixelFormatType_420YpCbCr8BiPlanarVideoRange,
                )
            ) {
                setVideoSettings(
                    mapOf(
                        kCVPixelBufferPixelFormatTypeKey to
                            kCVPixelBufferPixelFormatType_420YpCbCr8BiPlanarVideoRange,
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
