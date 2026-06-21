package com.micrantha.eyespie.features.things.data.source

import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.eyespie.features.things.data.model.ThingData
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface ThingsLocalSource {
    fun getAll(): Result<List<ThingData>>
    fun saveAll(things: List<ThingData>): Result<Unit>
}

class ThingsLocalSourceImpl(
    private val fileSystem: FileSystem,
    private val json: Json
) : ThingsLocalSource {
    private val thingsPath = fileSystem.filesPath().resolve("things.json")

    override fun getAll(): Result<List<ThingData>> = try {
        val data = fileSystem.fileRead(thingsPath)
        Result.success(json.decodeFromString(ListSerializer(ThingData.serializer()), data.decodeToString()))
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun saveAll(things: List<ThingData>): Result<Unit> = try {
        val data = json.encodeToString(ListSerializer(ThingData.serializer()), things).encodeToByteArray()
        fileSystem.fileWrite(thingsPath, data)
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
