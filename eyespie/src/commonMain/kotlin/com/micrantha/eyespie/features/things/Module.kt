package com.micrantha.eyespie.features.things

import com.micrantha.eyespie.domain.repository.ThingRepository
import com.micrantha.eyespie.features.things.data.ThingDataRepository
import com.micrantha.eyespie.features.things.data.mapping.ThingsDomainMapper
import com.micrantha.eyespie.features.things.data.source.SqlThingsLocalSource
import com.micrantha.eyespie.features.things.data.source.SupabaseThingsRemoteSource
import com.micrantha.eyespie.features.things.data.source.ThingsLocalSource
import com.micrantha.eyespie.features.things.data.source.ThingsRemoteSource
import com.micrantha.eyespie.features.things.ui.detail.ThingDetailEnvironment
import com.micrantha.eyespie.features.things.ui.detail.ThingDetailScreen
import com.micrantha.eyespie.features.things.ui.detail.ThingDetailScreenModel
import org.kodein.di.DI
import org.kodein.di.bindProviderOf
import org.kodein.di.delegate


internal fun module() = DI.Module("Things") {
    bindProviderOf(::ThingsDomainMapper)
    bindProviderOf(::ThingDataRepository)
    delegate<ThingRepository>().to<ThingDataRepository>()
    bindProviderOf(::SupabaseThingsRemoteSource)
    delegate<ThingsRemoteSource>().to<SupabaseThingsRemoteSource>()
    bindProviderOf(::SqlThingsLocalSource)
    delegate<ThingsLocalSource>().to<SqlThingsLocalSource>()

    bindProviderOf(::ThingDetailScreen)
    bindProviderOf(::ThingDetailScreenModel)
    bindProviderOf(::ThingDetailEnvironment)
}
