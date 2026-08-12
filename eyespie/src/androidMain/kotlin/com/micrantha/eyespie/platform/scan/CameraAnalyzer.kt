package com.micrantha.eyespie.platform.scan

import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.micrantha.bluebell.observability.logger
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CameraAnalyzer(
    private val regionOfInterest: RectF? = null,
    private val callback: CameraScannerDispatch,
    private val errorCallback: (Throwable) -> Unit,
    private val scope: CoroutineScope
) : ImageAnalysis.Analyzer {
    private val log by logger()
    private val analysisInFlight = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        if (!analysisInFlight.compareAndSet(false, true)) {
            image.close()
            return
        }

        val frame = try {
            // CameraX owns ImageProxy and its wrapped Media.Image. Copy the pixels into an
            // app-owned Bitmap before crossing the coroutine boundary, then release the proxy.
            PlatformCameraImage(
                _bitmap = image.toBitmap(),
                _width = image.width,
                _height = image.height,
                _rotation = image.imageInfo.rotationDegrees,
                _timestamp = image.imageInfo.timestamp,
                regionOfInterest = regionOfInterest
            )
        } catch (err: Throwable) {
            analysisInFlight.set(false)
            errorCallback(err)
            log.error(err) { "unable to prepare camera image for analysis" }
            return
        } finally {
            image.close()
        }

        scope.launch {
            try {
                callback(frame)
            } catch (err: Throwable) {
                errorCallback(err)
                log.error(err) { "unable to analyze camera image" }
            }
        }.invokeOnCompletion {
            analysisInFlight.set(false)
        }
    }
}
