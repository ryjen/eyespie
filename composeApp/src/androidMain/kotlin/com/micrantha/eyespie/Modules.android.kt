package com.micrantha.eyespie

import android.content.Context
import com.micrantha.bluebell.get
import com.micrantha.bluebell.platform.AndroidNetworkMonitor
import com.micrantha.bluebell.platform.BackgroundDownloadManager
import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCase
import com.micrantha.eyespie.platform.scan.analyzer.DetectCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.DominantColorCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.LabelCaptureAnalyzer
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.bindInstance
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingletonOf

fun androidDependencies(
    context: Context,
) = DI {
    bindInstance { context }

    bindSingletonOf(::Platform)

    bindProviderOf(::AndroidNetworkMonitor)

    bindProviderOf(::LabelCaptureAnalyzer)
    bindProviderOf(::DominantColorCaptureAnalyzer)
    bindProviderOf(::DetectCaptureAnalyzer)

    bindProviderOf(::BackgroundDownloadManager)

    bindProviderOf(::LoadCameraImageUseCase)

    bindSingletonOf(::GenAI)

    bindFactory { namespace: String ->
        BackgroundDownloadManager(
            get(), get(), namespace
        )
    }
}
