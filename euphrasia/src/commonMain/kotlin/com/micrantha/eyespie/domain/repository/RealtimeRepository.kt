package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.RealtimeAction
import com.micrantha.eyespie.domain.entities.Thing
import kotlinx.coroutines.flow.Flow

interface RealtimeRepository {

    suspend fun start()

    fun stop()

    suspend fun pause()

    fun things(): Flow<RealtimeAction<Thing>>
}
