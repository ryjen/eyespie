package com.micrantha.eyespie.core

import com.cactus.CactusVLM
import com.micrantha.eyespie.core.data.account.AccountDataRepository
import com.micrantha.eyespie.core.data.account.mapping.AccountDomainMapper
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.core.data.account.source.AccountRemoteSource
import com.micrantha.eyespie.core.data.ai.AiDataRepository
import com.micrantha.eyespie.core.data.ai.ClueDataRepository
import com.micrantha.eyespie.core.data.ai.mapping.ClueDataMapper
import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.core.data.ai.source.LLMLocalSource
import com.micrantha.eyespie.core.data.ai.source.ModelSource
import com.micrantha.eyespie.core.data.client.SupaClient
import com.micrantha.eyespie.core.data.client.SupaRealtimeClient
import com.micrantha.eyespie.core.data.storage.StorageDataRepository
import com.micrantha.eyespie.core.data.storage.source.CacheLocalSource
import com.micrantha.eyespie.core.data.storage.source.StorageRemoteSource
import com.micrantha.eyespie.core.data.system.LocationDataRepository
import com.micrantha.eyespie.core.data.system.RealtimeDataRepository
import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.core.data.system.mapping.RealtimeDomainMapper
import com.micrantha.eyespie.core.data.system.source.LocationLocalSource
import com.micrantha.eyespie.core.data.system.source.RealtimeRemoteSource
import dev.icerock.moko.geo.LocationTracker
import org.kodein.di.DI
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingleton
import org.kodein.di.bindSingletonOf
import org.kodein.di.delegate

internal fun module() = DI.Module("Core Feature") {
    bindSingletonOf(::SupaClient)
    bindSingletonOf(::SupaRealtimeClient)

    bindProviderOf(::AccountRemoteSource)
    bindProviderOf(::AccountDataRepository)

    bindProviderOf(::AccountDomainMapper)

    bindSingleton { CurrentSession }

    bindProviderOf(::CacheLocalSource)
    bindProviderOf(::StorageRemoteSource)
    bindProviderOf(::StorageDataRepository)

    bindProviderOf(::LocationDomainMapper)
    bindProviderOf(::LocationDataRepository)
    bindProviderOf(::RealtimeDataRepository)
    bindProviderOf(::RealtimeRemoteSource)
    bindProviderOf(::RealtimeDomainMapper)

    bindProviderOf(::AiDataRepository)
    bindProviderOf(::ClueDataRepository)
    bindProviderOf(::ClueDataMapper)
    bindProviderOf(::ModelSource)
    bindProviderOf(::LLMLocalSource)
    bindProviderOf(::CluePromptSource)

    bindSingleton { CactusVLM() }

    delegate<LocationLocalSource>().to<LocationTracker>()
}
