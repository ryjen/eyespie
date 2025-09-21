package com.micrantha.eyespie.platform.scan.analyzer

import android.graphics.Color
import androidx.palette.graphics.Palette
import com.micrantha.eyespie.features.scan.data.analyzer.DominantColor
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.components.CaptureAnalyzer

actual class DominantColorCaptureAnalyzer: CaptureAnalyzer<DominantColor> {

    actual override suspend fun analyze(image: CameraImage): Result<DominantColor> =
        try {
            val palette = Palette.from(image.toBitmap()).generate()

            val rgb = palette.dominantSwatch?.rgb ?: throw IllegalStateException("No dominant color found")

            val c1 = arrayOf(Color.red(rgb), Color.green(rgb), Color.blue(rgb))

            Result.success(c1)
        } catch (err: Throwable) {
            Result.failure(err)
        }
}
