package com.micrantha.eyespie.platform.scan

import android.graphics.RectF
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.micrantha.bluebell.app.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CameraAnalyzer(
    private val regionOfInterest: RectF? = null,
    private val callback: CameraScannerDispatch,
    private val scope: CoroutineScope
) : ImageAnalysis.Analyzer {
    private lateinit var current: CameraImage

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        try {
            if (!::current.isInitialized) {
                current = CameraImage(
                    _image = image.image,
                    _width = image.width,
                    _height = image.height,
                    _rotation = image.imageInfo.rotationDegrees,
                    _timestamp = image.imageInfo.timestamp,
                    regionOfInterest = regionOfInterest
                )
            } else {
                current.copy(image, regionOfInterest)
            }

            scope.launch {
                callback(current)
                //image.close()
            }
        } catch (err: Throwable) {
            Log.e("analyzer", err) { "unable to analyze camera image" }
        }
    }

}
