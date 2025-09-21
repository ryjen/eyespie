package com.micrantha.eyespie.features.scan

import com.micrantha.bluebell.get
import com.micrantha.eyespie.features.scan.data.analyzer.ColorCaptureAnalyzer
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureEnvironment
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureScreen
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureScreenModel
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureStateMapper
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditEnvironment
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditParams
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreen
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreenModel
import com.micrantha.eyespie.features.scan.usecase.LoadImageUseCase
import com.micrantha.eyespie.features.scan.usecase.TakeCaptureUseCase
import com.micrantha.eyespie.features.scan.usecase.UploadCaptureUseCase
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.bindProviderOf


internal fun module() = DI.Module("Scan") {

    bindProviderOf(::UploadCaptureUseCase)
    bindProviderOf(::LoadImageUseCase)
    bindProviderOf(::TakeCaptureUseCase)

    bindProviderOf(::ScanCaptureStateMapper)
    bindProviderOf(::ScanCaptureEnvironment)
    bindProviderOf(::ScanCaptureScreenModel)
    bindProviderOf(::ScanCaptureScreen)

    bindProviderOf(::ColorCaptureAnalyzer)

    bindProviderOf(::ScanEditEnvironment)
    bindProviderOf(::ScanEditScreenModel)
    bindFactory { params: ScanEditParams -> ScanEditScreen(get(), params) }
}
