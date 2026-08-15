package com.micrantha.eyespie.platform.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.micrantha.bluebell.observability.logger
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.AiProof
import kotlinx.coroutines.tasks.await
import okio.Path
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import java.io.File

class MlKitImageLabeler(
    private val context: Context
) : PlatformImageLabeler {

    private val log by logger()

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    override suspend fun label(image: Path): Result<AiProof> = try {
        val file = File(image.toString())
        if (!file.exists()) {
            throw IllegalStateException("image file does not exist at $image")
        }
        val inputImage = InputImage.fromFilePath(context, Uri.fromFile(file))
        val labels = labeler.process(inputImage).await()

        log.debug { "ML Kit found ${labels.size} labels for $image" }

        Result.success(
            labels.map {
                AiClue(
                    data = it.text,
                    confidence = it.confidence,
                    answer = it.text
                )
            }.toSet()
        )
    } catch (e: Exception) {
        log.error(e) { "ML Kit labeling failed for $image" }
        Result.failure(e)
    }
}

actual fun platformImageLabeler(di: DI): PlatformImageLabeler = 
    MlKitImageLabeler(di.direct.instance())
