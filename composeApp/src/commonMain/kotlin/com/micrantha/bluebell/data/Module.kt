package com.micrantha.bluebell.data

import com.micrantha.bluebell.data.download.DownloadService
import org.kodein.di.DI
import org.kodein.di.bindProviderOf

internal fun bluebellData() = DI.Module(name = "Bluebell Data") {
    bindProviderOf(::DownloadService)
}
