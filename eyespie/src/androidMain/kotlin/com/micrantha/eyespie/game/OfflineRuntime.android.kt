package com.micrantha.eyespie.game

import android.content.Context
import com.micrantha.eyespie.identity.LocalPlayerIdentityRepository
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_FILE
import com.micrantha.eyespie.imaging.ImageEmbeddingGenerator
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.persistence.AndroidEyespieDatabaseFactory
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import java.nio.ByteBuffer
import java.util.UUID

fun createAndroidOfflineRuntime(context: Context): OfflineRuntimeState = try {
    val applicationContext = context.applicationContext
    val database = AndroidEyespieDatabaseFactory(applicationContext).create()
    OfflineRuntimeState.Ready(
        OfflineGameCoordinator(
            identityRepository = LocalPlayerIdentityRepository(),
            gameRepository = SqlGameRepository(database),
            progressRepository = SqlThingProgressRepository(database),
            embeddingGenerator = LazyPackagedImageEmbeddingGenerator(applicationContext),
            idGenerator = { UUID.randomUUID().toString() },
        ),
    )
} catch (_: Throwable) {
    OfflineRuntimeState.Unavailable(OfflineRuntimeUnavailableReason.LOCAL_STORAGE_UNAVAILABLE)
}

/**
 * Defers model I/O and MediaPipe initialization until the coordinator moves work off the UI thread.
 * A missing/corrupt packaged model fails through the typed embedding path; it never authorizes a
 * download or remote inference fallback.
 */
private class LazyPackagedImageEmbeddingGenerator(
    private val context: Context,
) : ImageEmbeddingGenerator {
    private val delegate: ImageEmbeddingGenerator by lazy {
        MediaPipeImageEmbeddingGenerator(
            context = context,
            modelBuffer = loadImageEmbedderModel(context),
        )
    }

    override suspend fun generate(image: CapturedImage): List<Float> = delegate.generate(image)
}

private fun loadImageEmbedderModel(context: Context): ByteBuffer {
    val bytes = context.assets.open(IMAGE_EMBEDDER_MODEL_FILE).use { it.readBytes() }
    return ByteBuffer.allocateDirect(bytes.size).apply {
        put(bytes)
        rewind()
    }
}
