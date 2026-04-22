package com.micrantha.eyespie.core.data.observability

import com.micrantha.bluebell.observability.repository.destination.SupabaseInsertClient
import com.micrantha.eyespie.core.data.client.SupaClient
import io.github.jan.supabase.postgrest.postgrest

/**
 * Adapts the app's [SupaClient] (Supabase-KT) to Bluebell's minimal [SupabaseInsertClient] interface.
 */
class SupabaseInsertClientAdapter(
    private val supaClient: SupaClient,
) : SupabaseInsertClient {

    override suspend fun insert(table: String, rows: List<Map<String, Any?>>) {
        // Use the public PostgREST handle.
        supaClient.supabase.postgrest[table].insert(rows)
    }
}
