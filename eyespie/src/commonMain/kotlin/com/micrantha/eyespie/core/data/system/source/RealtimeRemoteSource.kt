package com.micrantha.eyespie.core.data.system.source

import com.micrantha.eyespie.core.data.client.SupaRealtimeClient
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.Flow

internal interface RealtimeRemoteSource {
    suspend fun connect()
    suspend fun block()
    fun disconnect()
    fun subscribe(table: String): Flow<PostgresAction>
}

internal class SupabaseRealtimeRemoteSource(
    private val supaRealtimeClient: SupaRealtimeClient
) : RealtimeRemoteSource {
    override suspend fun connect() = supaRealtimeClient.connect()

    override suspend fun block() = supaRealtimeClient.block()

    override fun disconnect() = supaRealtimeClient.disconnect()

    override fun subscribe(table: String) = supaRealtimeClient.subscribe(table)
}
