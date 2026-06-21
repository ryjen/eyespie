package com.micrantha.eyespie.features.things

import com.micrantha.eyespie.features.things.data.ThingDataRepository
import com.micrantha.eyespie.features.things.data.mapping.ThingsDomainMapper
import com.micrantha.eyespie.features.things.data.source.ThingsLocalSource
import com.micrantha.eyespie.features.things.data.source.ThingsLocalSourceImpl
import com.micrantha.eyespie.features.things.data.source.ThingsRemoteSource
import com.micrantha.eyespie.features.things.data.source.ThingsRemoteSourceImpl
import com.micrantha.eyespie.features.things.ui.detail.ThingDetailEnvironment
import com.micrantha.eyespie.features.things.ui.detail.ThingDetailScreen
import com.micrantha.eyespie.features.things.ui.detail.ThingDetailScreenModel
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.instance


internal fun module() = DI.Module("Things") {
    bindProviderOf(::ThingDataRepository)
    bindProvider<ThingsRemoteSource> { ThingsRemoteSourceImpl(instance()) }
    bindProvider<ThingsLocalSource> { ThingsLocalSourceImpl(instance(), instance()) }
    bindProviderOf(::ThingsDomainMapper)

    bindProviderOf(::ThingDetailScreen)
    bindProviderOf(::ThingDetailScreenModel)
    bindProviderOf(::ThingDetailEnvironment)
}
