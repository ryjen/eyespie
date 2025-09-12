package com.micrantha.eyespie

import android.content.Context
import com.cactus.CactusLM
import com.micrantha.bluebell.platform.AndroidNetworkMonitor
import com.micrantha.bluebell.platform.BackgroundDownloadManager
import com.micrantha.bluebell.platform.Platform
import org.kodein.di.DI
import org.kodein.di.bindInstance
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingletonOf

fun androidDependencies(
    context: Context,
) = DI {
    bindInstance { context }

    bindInstance { CactusLM() }

    bindSingletonOf(::Platform)

    bindProviderOf(::AndroidNetworkMonitor)

    bindSingletonOf(::BackgroundDownloadManager)
}
