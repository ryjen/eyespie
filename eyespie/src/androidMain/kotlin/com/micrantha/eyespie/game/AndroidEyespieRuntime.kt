package com.micrantha.eyespie.game

import android.content.Context
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.identity.LocalPlayerIdentityRepository
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_FILE
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.persistence.AndroidEyespieDatabaseFactory
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import java.nio.ByteBuffer
import java.util.UUID

fun createAndroidEyespieRuntime(context: Context): EyespieRuntime {
    val applicationContext = context.applicationContext
    val database = AndroidEyespieDatabaseFactory(applicationContext).create()
    return EyespieRuntime(
        LocalGameLoop(
            identityRepository = LocalPlayerIdentityRepository(),
            gameRepository = SqlGameRepository(database),
            progressRepository = SqlThingProgressRepository(database),
            embeddingGenerator = MediaPipeImageEmbeddingGenerator(
                context = applicationContext,
                modelBuffer = loadImageEmbedderModel(applicationContext),
            ),
            idGenerator = AndroidLocalGameIdGenerator(),
        ),
    )
}

private class AndroidLocalGameIdGenerator : LocalGameIdGenerator {
    override fun nextGameId(): GameId = GameId("game:${UUID.randomUUID()}")
    override fun nextThingId(): ThingId = ThingId("thing:${UUID.randomUUID()}")
}

private fun loadImageEmbedderModel(context: Context): ByteBuffer {
    val bytes = context.assets.open(IMAGE_EMBEDDER_MODEL_FILE).use { it.readBytes() }
    check(bytes.isNotEmpty()) { "image embedder model asset is empty" }
    return ByteBuffer.allocateDirect(bytes.size).apply {
        put(bytes)
        rewind()
    }
}
