package com.micrantha.eyespie.features.things.data.source

import com.micrantha.eyespie.core.data.client.SupaClient
import com.micrantha.eyespie.features.things.data.model.MatchRequest
import com.micrantha.eyespie.features.things.data.model.MatchResponse
import com.micrantha.eyespie.features.things.data.model.NearbyRequest
import com.micrantha.eyespie.features.things.data.model.ThingData
import com.micrantha.eyespie.features.things.data.model.ThingListing
import com.micrantha.eyespie.features.things.data.model.ThingRequest
import com.micrantha.eyespie.features.things.data.model.ThingResponse
import io.github.jan.supabase.postgrest.query.Columns

internal interface ThingsRemoteSource {
    suspend fun save(data: ThingRequest): Result<ThingResponse>

    suspend fun things(playerID: String): Result<List<ThingListing>>

    suspend fun thing(thingID: String): Result<ThingResponse>

    suspend fun nearby(request: NearbyRequest): Result<List<ThingResponse>>

    suspend fun match(request: MatchRequest): Result<List<MatchResponse>>
}

internal class SupabaseThingsRemoteSource(
    private val supaClient: SupaClient,
) : ThingsRemoteSource {
    override suspend fun save(data: ThingRequest) = try {
        val result = supaClient.things().insert(data).decodeList<ThingResponse>()
        Result.success(result.first())
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override suspend fun things(playerID: String) = try {
        val result = supaClient.things().select(
            Columns.type<ThingData>()
        ) {
            filter {
                eq("created_by", playerID)
            }
        }.decodeList<ThingListing>()
        Result.success(result)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override suspend fun thing(thingID: String) = try {
        val result = supaClient.things().select(
            Columns.type<ThingData>()
        ) {
            filter {
                eq("id", thingID)
            }
        }.decodeSingle<ThingResponse>()
        Result.success(result)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override suspend fun nearby(request: NearbyRequest) = try {
        val res = supaClient.nearby(request).decodeList<ThingResponse>()
        Result.success(res)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override suspend fun match(request: MatchRequest) = try {
        val res = supaClient.match(request).decodeList<MatchResponse>()
        Result.success(res)
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
