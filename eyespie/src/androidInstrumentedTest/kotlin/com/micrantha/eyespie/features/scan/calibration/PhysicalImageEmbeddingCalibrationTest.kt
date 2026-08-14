package com.micrantha.eyespie.features.scan.calibration

import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.micrantha.eyespie.features.scan.usecase.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.platform.scan.PlatformCameraImage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PhysicalImageEmbeddingCalibrationTest {

    @Test
    fun collectProductionMediaPipeCalibrationReport() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val generator = MediaPipeImageEmbeddingGenerator(context)

        val fixtures = listOf(
            Triple("burger", "reference", "burger.jpg"),
            Triple("burger_crop", "related", "burger_crop.jpg"),
            Triple("burger_rotated", "related", "burger_rotated.jpg"),
            Triple("cat", "unrelated", "cat.jpg"),
        ).map { (id, role, fileName) ->
            val assetPath = "image-embedding-calibration/$fileName"
            val bitmap = instrumentation.context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: error("unable to decode calibration fixture: $fileName")
            try {
                val image = PlatformCameraImage(
                    _bitmap = bitmap,
                    _width = bitmap.width,
                    _height = bitmap.height,
                    _rotation = 0,
                )
                val runs = List(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT) {
                    generator.generate(image)
                }
                summarizeImageEmbeddingCalibrationFixture(id, role, runs)
            } finally {
                bitmap.recycle()
            }
        }

        assertEquals(4, fixtures.size)
        fixtures.forEach { fixture ->
            assertEquals(1024, fixture.embedding.size)
            assertEquals(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT, fixture.repeat_count)
            assertTrue(fixture.embedding.all(Float::isFinite))
        }

        val report = ImageEmbeddingCalibrationReport(
            platform = "android",
            device = ImageEmbeddingCalibrationDevice(
                manufacturer = Build.MANUFACTURER.ifBlank { "unknown" },
                model = Build.MODEL.ifBlank { "unknown" },
                os = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            ),
            runtime = ImageEmbeddingCalibrationRuntime(
                name = "mediapipe-tasks-vision",
                version = "0.10.35",
            ),
            fixtures = fixtures,
        )

        val output = File(context.filesDir, "image-embedding-calibration/android.json")
        output.parentFile?.mkdirs()
        output.writeText(report.toCalibrationJson())
        println("EYESPIE_IMAGE_EMBEDDING_CALIBRATION=${output.absolutePath}")
    }
}
