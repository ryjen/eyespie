package com.micrantha.eyespie.platform.scan

import android.graphics.RectF
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.micrantha.bluebell.observability.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CameraAnalyzer(
    private val regionOfInterest: RectF? = null,
    private val callback: CameraScannerDispatch,
    private val errorCallback: (Throwable) -> Unit,
    private val scope: CoroutineScope
) : ImageAnalysis.Analyzer {
    private val log by logger()

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val frame = CameraImage(
            _image = image.image,
            _width = image.width,
            _height = image.height,
            _rotation = image.imageInfo.rotationDegrees,
            _timestamp = image.imageInfo.timestamp,
            regionOfInterest = regionOfInterest
        )

        scope.launch {
            try {
                callback(frame)
            } catch (err: Throwable) {
                errorCallback(err)
                log.error(err) { "unable to analyze camera image" }
            } finally {
                image.close()
            }
        }
    }
}
