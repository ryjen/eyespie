package com.micrantha.eyespie.features.scan.calibration

import androidx.compose.ui.graphics.ImageBitmap
import com.micrantha.bluebell.domain.security.sha256
import com.micrantha.bluebell.platform.toByteArray
import com.micrantha.bluebell.platform.toImageBitmap
import com.micrantha.eyespie.app.AppConfig
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.features.scan.usecase.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.UIKit.UIDevice
import kotlin.Throws

/**
 * Explicit physical-device calibration entry point exported to the iOS wrapper.
 *
 * It is inert unless the wrapper calls [collect]. Fixture bytes come from the generation-pinned
 * debug resource set and inference goes through the production MediaPipe generator.
 */
class ImageEmbeddingCalibrationCollector {

    @Throws(Exception::class)
    fun collect(): String = runBlocking {
        val generator = MediaPipeImageEmbeddingGenerator()
        val fixtures = listOf(
            Triple("burger", "reference", "burger.jpg"),
            Triple("burger_crop", "related", "burger_crop.jpg"),
            Triple("burger_rotated", "related", "burger_rotated.jpg"),
            Triple("cat", "unrelated", "cat.jpg"),
        ).map { (id, role, fileName) ->
            val image = EncodedCalibrationCameraImage(loadFixture(fileName))
            val runs = List(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT) {
                generator.generate(image)
            }
            summarizeImageEmbeddingCalibrationFixture(id, role, runs)
        }

        val device = UIDevice.currentDevice
        ImageEmbeddingCalibrationReport(
            platform = "ios",
            device = ImageEmbeddingCalibrationDevice(
                manufacturer = "Apple",
                model = device.model(),
                os = "${device.systemName()} ${device.systemVersion}",
            ),
            runtime = ImageEmbeddingCalibrationRuntime(
                name = "EyespieMediaPipeTasksVision",
                version = "0.10.26.2",
            ),
            model = ImageEmbeddingCalibrationModel(
                sha256 = bundledModelSha256(),
            ),
            match_policy = ImageEmbeddingCalibrationMatchPolicy(
                cosine_threshold = AppConfig.MATCH_THRESHOLD.toFloat(),
            ),
            fixtures = fixtures,
        ).toCalibrationJson()
    }

    private fun loadFixture(fileName: String): ByteArray {
        val baseName = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.')
        val path = NSBundle.mainBundle.pathForResource(baseName, ofType = extension)
            ?: error("calibration fixture is unavailable: $fileName")
        val data = NSData.dataWithContentsOfFile(path)
            ?: error("calibration fixture cannot be read: $fileName")
        return data.toByteArray()
    }

    private fun bundledModelSha256(): String {
        val fileName = ImageEmbeddingContract.androidModelAssetName
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        val baseName = fileName.removeSuffix(if (extension.isEmpty()) "" else ".$extension")
        if (baseName.isEmpty() || extension.isEmpty()) {
            error("image embedder model resource name is invalid")
        }
        val path = NSBundle.mainBundle.pathForResource(baseName, ofType = extension)
            ?: error("image embedder model resource is unavailable")
        return sha256(FileSystem.SYSTEM.source(path.toPath()))
    }
}

private class EncodedCalibrationCameraImage(
    private val bytes: ByteArray,
) : CameraImage {
    private val bitmap: ImageBitmap = bytes.toImageBitmap()

    override val width: Int = bitmap.width
    override val height: Int = bitmap.height

    override fun toByteArray(): ByteArray = bytes

    override fun toImageBitmap(): ImageBitmap = bitmap
}
