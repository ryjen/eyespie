package com.micrantha.eyespie.core


import com.micrantha.bluebell.get
import com.micrantha.bluebell.observability.domain.SupabaseInsertClient
import com.micrantha.bluebell.observability.domain.UsageObservability
import com.micrantha.bluebell.observability.repository.DefaultDestinationContextProvider
import com.micrantha.bluebell.observability.repository.OfflineSupabaseUsageObservability
import com.micrantha.bluebell.observability.repository.OkioJsonLinesDiskCache
import com.micrantha.bluebell.observability.usecase.FlushOfflineUsageToSupabase
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.core.data.account.AccountDataRepository
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.core.data.account.source.AccountRemoteSource
import com.micrantha.eyespie.core.data.account.source.SupabaseAccountRemoteSource
import com.micrantha.eyespie.core.data.ai.ClueDataRepository
import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.core.data.client.SupaClient
import com.micrantha.eyespie.core.data.client.SupaRealtimeClient
import com.micrantha.eyespie.core.data.db.DatabaseDriverFactory
import com.micrantha.eyespie.core.data.observability.SupabaseInsertClientAdapter
import com.micrantha.eyespie.core.data.storage.StorageDataRepository
import com.micrantha.eyespie.core.data.storage.source.CacheLocalSource
import com.micrantha.eyespie.core.data.storage.source.DefaultCacheLocalSource
import com.micrantha.eyespie.core.data.storage.source.PreferencesLocalSource
import com.micrantha.eyespie.core.data.storage.source.StorageRemoteSource
import com.micrantha.eyespie.core.data.storage.source.SupabaseStorageRemoteSource
import com.micrantha.eyespie.core.data.system.LocationDataRepository
import com.micrantha.eyespie.core.data.system.RealtimeDataRepository
import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.core.data.system.mapping.RealtimeDomainMapper
import com.micrantha.eyespie.core.data.system.source.LocationLocalSource
import com.micrantha.eyespie.core.data.system.source.MokoLocationLocalSource
import com.micrantha.eyespie.core.data.system.source.RealtimeRemoteSource
import com.micrantha.eyespie.core.data.system.source.SupabaseRealtimeRemoteSource
import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.domain.repository.AccountRepository
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.domain.repository.LocationRepository
import com.micrantha.eyespie.domain.repository.StorageRepository
import dev.icerock.moko.geo.LocationTracker
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingleton
import org.kodein.di.bindSingletonOf
import org.kodein.di.delegate
import org.kodein.di.instance
import okio.FileSystem as OkioFileSystem

internal fun module() = DI.Module("Core Feature") {
    bindSingletonOf(::SupaClient)
    bindSingletonOf(::SupaRealtimeClient)

    bindSingleton {
        EyesPieDatabase(instance<DatabaseDriverFactory>().createDriver())
    }

    bindProviderOf(::SupabaseAccountRemoteSource)
    delegate<AccountRemoteSource>().to<SupabaseAccountRemoteSource>()
    bindProviderOf(::AccountDataRepository)
    delegate<AccountRepository>().to<AccountDataRepository>()

    bindSingleton { CurrentSession }

    bindProviderOf(::DefaultCacheLocalSource)
    delegate<CacheLocalSource>().to<DefaultCacheLocalSource>()
    bindProviderOf(::SupabaseStorageRemoteSource)
    delegate<StorageRemoteSource>().to<SupabaseStorageRemoteSource>()
    bindProviderOf(::StorageDataRepository)
    delegate<StorageRepository>().to<StorageDataRepository>()
    bindSingletonOf(::PreferencesLocalSource)

    bindProviderOf(::LocationDomainMapper)
    bindProvider { MokoLocationLocalSource(instance<LocationTracker>()) }
    delegate<LocationLocalSource>().to<MokoLocationLocalSource>()
    bindProviderOf(::LocationDataRepository)
    delegate<LocationRepository>().to<LocationDataRepository>()
    bindProviderOf(::SupabaseRealtimeRemoteSource)
    delegate<RealtimeRemoteSource>().to<SupabaseRealtimeRemoteSource>()
    bindProviderOf(::RealtimeDataRepository)
    bindProviderOf(::RealtimeDomainMapper)

    bindProvider { ClueDataRepository(get(), get()) }
    delegate<ClueRepository>().to<ClueDataRepository>()
    bindProviderOf(::CluePromptSource)

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
