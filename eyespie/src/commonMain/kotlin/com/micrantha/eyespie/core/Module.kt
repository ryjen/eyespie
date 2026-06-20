package com.micrantha.eyespie.core

import com.micrantha.bluebell.get
import com.micrantha.bluebell.observability.domain.*
import com.micrantha.bluebell.observability.entity.*
import com.micrantha.bluebell.observability.repository.*
import com.micrantha.bluebell.platform.Platform
import com.micrantha.bluebell.observability.usecase.FlushOfflineUsageToSupabase


import com.micrantha.eyespie.core.data.account.AccountDataRepository
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.core.data.account.source.AccountRemoteSource
import com.micrantha.eyespie.core.data.ai.ClueDataRepository
import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.core.data.client.SupaClient
import com.micrantha.eyespie.core.data.client.SupaRealtimeClient
import com.micrantha.eyespie.core.data.observability.SupabaseInsertClientAdapter
import com.micrantha.eyespie.core.data.storage.StorageDataRepository
import com.micrantha.eyespie.core.data.storage.source.CacheLocalSource
import com.micrantha.eyespie.core.data.storage.source.PreferencesLocalSource
import com.micrantha.eyespie.core.data.storage.source.StorageRemoteSource
import com.micrantha.eyespie.core.data.system.LocationDataRepository
import com.micrantha.eyespie.core.data.system.RealtimeDataRepository
import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.core.data.system.mapping.RealtimeDomainMapper
import com.micrantha.eyespie.core.data.system.source.LocationLocalSource
import com.micrantha.eyespie.core.data.system.source.RealtimeRemoteSource
import dev.icerock.moko.geo.LocationTracker
import okio.FileSystem as OkioFileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingleton
import org.kodein.di.bindSingletonOf
import org.kodein.di.delegate
import org.kodein.di.instance

internal fun module() = DI.Module("Core Feature") {
    bindSingletonOf(::SupaClient)
    bindSingletonOf(::SupaRealtimeClient)

    bindProviderOf(::AccountRemoteSource)
    bindProviderOf(::AccountDataRepository)

    bindSingleton { CurrentSession }

    bindProviderOf(::CacheLocalSource)
    bindProviderOf(::StorageRemoteSource)
    bindProviderOf(::StorageDataRepository)
    bindSingletonOf(::PreferencesLocalSource)

    bindProviderOf(::LocationDomainMapper)
    bindProviderOf(::LocationDataRepository)
    bindProviderOf(::RealtimeDataRepository)
    bindProviderOf(::RealtimeRemoteSource)
    bindProviderOf(::RealtimeDomainMapper)

    bindProvider { ClueDataRepository(get(), get()) }
    bindProviderOf(::CluePromptSource)

    delegate<LocationLocalSource>().to<LocationTracker>()

    bindSingletonOf(::SupabaseInsertClientAdapter)
    bindSingleton<SupabaseInsertClient> { instance<SupabaseInsertClientAdapter>() }

    bindSingleton {
        val platform = instance<Platform>()
        val dir = platform.filesPath()
        OkioJsonLinesDiskCache(
            fileSystem = OkioFileSystem.SYSTEM,
            filePath = (dir.toString() + "/observability-events.jsonl").toPath(),
        )
    }

    bindSingleton {
        FlushOfflineUsageToSupabase(
            diskCache = instance(),
            supabase = instance(),
            table = "usage_events",
        )
    }

    bindSingleton<UsageObservability> {
        OfflineSupabaseUsageObservability(
            diskCache = instance(),
            flushToSupabase = instance(),
            contextProvider = DefaultDestinationContextProvider.create(),
        )
    }
}
