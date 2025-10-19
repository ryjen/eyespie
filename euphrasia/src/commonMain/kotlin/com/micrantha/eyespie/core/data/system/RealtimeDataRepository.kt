package com.micrantha.eyespie.core.data.system

import com.micrantha.eyespie.core.data.system.mapping.RealtimeDomainMapper
import com.micrantha.eyespie.core.data.system.source.RealtimeRemoteSource
import com.micrantha.eyespie.domain.repository.RealtimeRepository
import kotlinx.coroutines.flow.map

class RealtimeDataRepository(
    private val remoteSource: RealtimeRemoteSource,
    private val mapper: RealtimeDomainMapper
) : RealtimeRepository {

    override suspend fun start() = remoteSource.connect()

    override fun stop() = remoteSource.disconnect()

    override suspend fun pause() = remoteSource.block()

    override fun things() = remoteSource.subscribe("Thing").map(mapper::thing)
}
