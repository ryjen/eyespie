package com.micrantha.eyespie.features.scan

import com.micrantha.bluebell.get
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureEnvironment
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureScreen
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureScreenModel
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureStateMapper
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditEnvironment
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreen
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreenModel
import com.micrantha.eyespie.features.scan.ui.usecase.AnalyzeCaptureUseCase
import com.micrantha.eyespie.features.scan.ui.usecase.GetEditCaptureUseCase
import com.micrantha.eyespie.features.scan.ui.usecase.UploadCaptureUseCase
import okio.Path
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.bindProviderOf


internal fun module() = DI.Module("Scan") {

    bindProviderOf(::UploadCaptureUseCase)
    bindProviderOf(::AnalyzeCaptureUseCase)
    bindProviderOf(::GetEditCaptureUseCase)

    bindProviderOf(::ScanCaptureStateMapper)
    bindProviderOf(::ScanCaptureEnvironment)
    bindProviderOf(::ScanCaptureScreenModel)
    bindProviderOf(::ScanCaptureScreen)

    bindProviderOf(::ScanEditEnvironment)
    bindProviderOf(::ScanEditScreenModel)
    bindFactory { image: Path -> ScanEditScreen(get(), image) }
}
