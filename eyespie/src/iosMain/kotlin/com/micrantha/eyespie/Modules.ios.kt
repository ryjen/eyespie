package com.micrantha.eyespie

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.Platform
import com.micrantha.bluebell.platform.PlatformGenAI
import com.micrantha.eyespie.core.data.db.DatabaseDriverFactory
import com.micrantha.eyespie.platform.scan.analyzer.DetectCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.DominantColorCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.LabelCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.generator.ImageObfuscator
import com.micrantha.eyespie.platform.scan.generator.ImageStyler
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingleton
import org.kodein.di.bindSingletonOf
import org.kodein.di.delegate

fun iosModules(app: AppDelegate) = DI {

    bindSingleton { Platform(app.networkMonitor) }

    bindSingletonOf(::PlatformGenAI)
    delegate<GenAI>().to<PlatformGenAI>()

    bindSingletonOf(::DatabaseDriverFactory)

    bindProvider { app.networkMonitor }

    bindProviderOf(::LabelCaptureAnalyzer)
    bindProviderOf(::DominantColorCaptureAnalyzer)
    bindProviderOf(::DetectCaptureAnalyzer)

    bindProviderOf(::ImageObfuscator)
    bindProviderOf(::ImageStyler)
}
