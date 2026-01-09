package com.micrantha.eyespie

import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.platform.scan.analyzer.DetectCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.DominantColorCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.LabelCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.generator.ImageObfuscator
import com.micrantha.eyespie.platform.scan.generator.ImageStyler
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingleton

fun iosModules(app: AppDelegate) = DI {

    bindSingleton { Platform(app.networkMonitor) }

    bindProvider { app.networkMonitor }

    bindProviderOf(::LabelCaptureAnalyzer)
    bindProviderOf(::DominantColorCaptureAnalyzer)
    bindProviderOf(::DetectCaptureAnalyzer)

    bindProviderOf(::ImageObfuscator)
    bindProviderOf(::ImageStyler)
}
