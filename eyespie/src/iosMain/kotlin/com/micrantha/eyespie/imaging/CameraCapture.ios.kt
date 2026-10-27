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
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.micrantha.eyespie.telemetry.LocalOperationalTelemetry
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVImageBufferRef
import platform.CoreVideo.CVPixelBufferRelease
import platform.CoreVideo.CVPixelBufferRetain
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSLock
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.ImageIO.CGImagePropertyOrientation
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationRight
import platform.ImageIO.kCGImagePropertyOrientationUp
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.resume

private val CAMERA_PIXEL_FORMAT_PREFERENCE = listOf(
    kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange,
    kCVPixelFormatType_420YpCbCr8BiPlanarFullRange,
    kCVPixelFormatType_32BGRA,
)

@Composable
actual fun CameraCapture(
    modifier: Modifier,
    onAvailabilityChanged: (CameraAvailability) -> Unit,
    onCameraError: (Throwable) -> Unit,
    onCaptured: (CapturedImage) -> Unit,
    captureButton: @Composable ((capture: () -> Unit) -> Unit),
    recoveryButton: @Composable ((openSettings: () -> Unit) -> Unit),
) {
    val telemetry = LocalOperationalTelemetry.current
    val compositionScope = rememberCoroutineScope()
    var authorization by remember { mutableStateOf(currentCameraAvailability()) }

    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            null,
            null,
        ) {
            authorization = currentCameraAvailability()
        }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }

    LaunchedEffect(authorization) {
        if (authorization == CameraAvailability.Requestable) {
            authorization = requestCameraAccess()
        }
    }

    val device = remember(authorization) {
        if (authorization == CameraAvailability.Ready) {
            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        } else {
            null
        }
    }
    val effectiveAvailability = if (
        authorization == CameraAvailability.Ready && device == null
    ) CameraAvailability.Unavailable else authorization
    LaunchedEffect(effectiveAvailability) { onAvailabilityChanged(effectiveAvailability) }

    // Callback lambdas may change during recomposition; only the physical device controls
    // session identity. Recreating the stream on an overlay state change restarts preview.
    val stream = remember(device) { device?.let(::CameraStream) }

    val imageCapture = remember(stream, telemetry) {
        stream?.let { TelemetryImageCapture(delegate = StreamImageCapture(it), telemetry = telemetry) }
    }

    if (stream != null) {
        DisposableEffect(stream) {
            runCatching {
                stream.setup()
                stream.start()
            }.onFailure(onCameraError)

            onDispose { stream.stop() }
        }

        // The native preview is display-only. Forwarding taps to UIKit makes Compose's
        // shutter/back controls compete with the interop touch arbitration delay.
        UIKitView(
            factory = stream::preview,
            modifier = modifier,
            properties = UIKitInteropProperties(isInteractive = false),
        )
    }

    when (effectiveAvailability) {
        CameraAvailability.Requestable -> Unit
        CameraAvailability.PermissionDenied -> recoveryButton {
            val settingsUrl = NSURL(string = UIApplicationOpenSettingsURLString)
            if (settingsUrl != null) {
                UIApplication.sharedApplication.openURL(settingsUrl)
            }
        }
        CameraAvailability.Unavailable -> Unit
        CameraAvailability.Ready -> if (imageCapture != null) {
            captureButton {
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
    }
}

private fun currentCameraAvailability(): CameraAvailability =
    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> CameraAvailability.Ready
        AVAuthorizationStatusNotDetermined -> CameraAvailability.Requestable
        AVAuthorizationStatusDenied -> CameraAvailability.PermissionDenied
        AVAuthorizationStatusRestricted -> CameraAvailability.Unavailable
        else -> CameraAvailability.Unavailable
    }

private suspend fun requestCameraAccess(): CameraAvailability =
    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> CameraAvailability.Ready
        AVAuthorizationStatusNotDetermined -> suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(
                mediaType = AVMediaTypeVideo,
            ) { granted: Boolean ->
                if (continuation.isActive) {
                    continuation.resume(
                        if (granted) CameraAvailability.Ready else CameraAvailability.PermissionDenied,
                    )
                }
            }
        }
        AVAuthorizationStatusDenied -> CameraAvailability.PermissionDenied
        AVAuthorizationStatusRestricted -> CameraAvailability.Unavailable
        else -> CameraAvailability.Unavailable
    }

/**
 * Exactly one native pixel buffer is retained while the camera is live. A capture takes its own
 * retain under the same lock so a concurrent frame replacement or stop cannot release its data.
 */
private data class RetainedCameraFrame(
    val pixelBuffer: CVImageBufferRef,
    val orientation: CGImagePropertyOrientation,
)

// Kotlin/Native cannot mix a Kotlin interface with ObjC superclass/protocol inheritance.
private class StreamImageCapture(private val stream: CameraStream) : ImageCapture {
    override suspend fun capture(): CapturedImage = stream.capture()
}

private class CameraStream(
    private val device: AVCaptureDevice,
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    private val session = AVCaptureSession()
    private val dispatchQueue = dispatch_queue_create("eyespie.camera.frames", null)
    private val frameLock = NSLock()
    private var latestFrame: RetainedCameraFrame? = null
    private var captureInFlight = false
    private var active = true

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        val pixelBuffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: return
        val orientation = when (fromConnection.videoOrientation) {
            AVCaptureVideoOrientationPortraitUpsideDown -> kCGImagePropertyOrientationDown
            AVCaptureVideoOrientationLandscapeLeft -> kCGImagePropertyOrientationLeft
            AVCaptureVideoOrientationLandscapeRight -> kCGImagePropertyOrientationRight
            else -> kCGImagePropertyOrientationUp
        }

        // This delegate's hot path performs only a bounded retain/swap/release, never YUV
        // conversion, BGRA rotation, PNG encoding, or coroutine scheduling per frame.
        frameLock.lock()
        val previous = try {
            if (!active) return
            CVPixelBufferRetain(pixelBuffer)
            val old = latestFrame
            latestFrame = RetainedCameraFrame(pixelBuffer, orientation)
            old
        } finally {
            frameLock.unlock()
        }
        previous?.let { CVPixelBufferRelease(it.pixelBuffer) }
    }

    suspend fun capture(): CapturedImage {
        val frame = frameLock.run {
            lock()
            try {
                check(active) { "camera session is stopped" }
                check(!captureInFlight) { "a camera capture is already in progress" }
                val current = latestFrame ?: error("no camera frame is available to capture")
                CVPixelBufferRetain(current.pixelBuffer)
                captureInFlight = true
                current
            } finally {
                unlock()
            }
        }
        return try {
            withContext(Dispatchers.Default) {
                currentCoroutineContext().ensureActive()
                copyCameraFrame(frame.pixelBuffer, frame.orientation).toCapturedImage()
            }
        } finally {
            CVPixelBufferRelease(frame.pixelBuffer)
            frameLock.lock()
            try {
                captureInFlight = false
            } finally {
                frameLock.unlock()
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
            alwaysDiscardsLateVideoFrames = true
            setSampleBufferDelegate(this@CameraStream, dispatchQueue)

            val availablePixelFormats = availableVideoCVPixelFormatTypes
                .mapNotNull(::cameraPixelFormatValue)
                .toSet()
            val selectedPixelFormat = CAMERA_PIXEL_FORMAT_PREFERENCE
                .firstOrNull(availablePixelFormats::contains)
                ?: throw IllegalStateException("camera exposes no supported pixel format")
            setVideoSettings(
                mapOf(kCVPixelBufferPixelFormatTypeKey to selectedPixelFormat),
            )
        }
        check(session.canAddOutput(output)) { "cannot add output to video session" }
        session.addOutput(output)
    }

    fun start() {
        dispatch_async(dispatchQueue) { session.startRunning() }
    }

    fun stop() {
        frameLock.lock()
        val previous = try {
            active = false
            val old = latestFrame
            latestFrame = null
            old
        } finally {
            frameLock.unlock()
        }
        previous?.let { CVPixelBufferRelease(it.pixelBuffer) }
        dispatch_async(dispatchQueue) { session.stopRunning() }
    }

    fun preview(): UIView = CameraPreviewView(session)
}

/**
 * Compose controls the UIView's frame. Keep the AVFoundation sublayer equal to the view's
 * current bounds on every UIKit layout pass; setting only an initial/resize callback frame
 * leaves a zero-sized preview layer on some iPhones despite live captured frames.
 */
private class CameraPreviewView(session: AVCaptureSession) : UIView(frame = CGRectZero.readValue()) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
        connection?.videoOrientation = AVCaptureVideoOrientationPortrait
    }

    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        try {
            CATransaction.setValue(true, kCATransactionDisableActions)
            previewLayer.setFrame(bounds)
        } finally {
            CATransaction.commit()
        }
    }
}


private fun cameraPixelFormatValue(value: Any?): UInt? =
    when (value) {
        is UInt -> value
        is NSNumber -> value.unsignedIntValue
        is Number -> value.toLong().toUInt()
        else -> null
    }
