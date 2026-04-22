package com.micrantha.eyespie

import android.content.Context
import com.micrantha.bluebell.get
import com.micrantha.bluebell.platform.AndroidNetworkMonitor
import com.micrantha.bluebell.platform.BackgroundDownloader
import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCase
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

    bindProviderOf(::LoadCameraImageUseCase)

    bindSingletonOf(::GenAI)

    bindFactory { namespace: String ->
        BackgroundDownloader(
            get(), get(), namespace
        )
    }
}
