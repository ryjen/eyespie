package com.micrantha.eyespie

import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.Platform
import com.micrantha.bluebell.platform.PlatformGenAI
import com.micrantha.bluebell.platform.PlatformImpl
import com.micrantha.eyespie.core.data.db.DatabaseDriverFactory
import com.micrantha.eyespie.model.iosModelAssetModule
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.bindSingletonOf
import org.kodein.di.delegate

fun iosModules(app: AppDelegate) = DI {

    import(iosModelAssetModule(app.modelDeliveryCapabilities))

    bindSingleton { PlatformImpl(app.networkMonitor) }
    delegate<Platform>().to<PlatformImpl>()
    delegate<FileSystem>().to<PlatformImpl>()

    bindSingletonOf(::PlatformGenAI)
    delegate<GenAI>().to<PlatformGenAI>()

    bindSingletonOf(::DatabaseDriverFactory)

    bindProvider { app.networkMonitor }
}
