package com.micrantha.eyespie.features.scan

import com.micrantha.eyespie.features.scan.data.CaptureSyncRepository
import com.micrantha.eyespie.features.scan.data.CaptureSyncRepositoryImpl
import com.micrantha.eyespie.features.scan.data.source.CaptureSyncSource
import com.micrantha.eyespie.features.scan.data.source.SqlCaptureSyncSource
import com.micrantha.eyespie.features.scan.entities.ScanEditParams
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureEnvironment
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureScreen
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureScreenModel
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureStateMapper
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditEnvironment
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreen
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreenModel
import com.micrantha.eyespie.features.scan.usecase.DeterministicImageEmbeddingGenerator
import com.micrantha.eyespie.features.scan.usecase.ImageEmbeddingGenerator
import com.micrantha.eyespie.features.scan.usecase.LoadImageUseCase
import com.micrantha.eyespie.features.scan.usecase.MatchCaptureUseCase
import com.micrantha.eyespie.features.scan.usecase.TakeCaptureUseCase
import com.micrantha.eyespie.features.scan.usecase.UploadCaptureUseCase
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingleton
import org.kodein.di.bindSingletonOf
import org.kodein.di.delegate
import org.kodein.di.instance


internal fun module() = DI.Module("Scan") {

    bindProviderOf(::SqlCaptureSyncSource)
    delegate<CaptureSyncSource>().to<SqlCaptureSyncSource>()
    bindSingletonOf(::CaptureSyncRepositoryImpl)
    delegate<CaptureSyncRepository>().to<CaptureSyncRepositoryImpl>()

    bindProvider<ImageEmbeddingGenerator> { DeterministicImageEmbeddingGenerator() }
    bindProviderOf(::MatchCaptureUseCase)
    bindProviderOf(::UploadCaptureUseCase)
    bindProviderOf(::LoadImageUseCase)
    bindProviderOf(::TakeCaptureUseCase)

    bindProviderOf(::ScanCaptureStateMapper)
    bindProviderOf(::ScanCaptureEnvironment)
    bindProviderOf(::ScanCaptureScreenModel)
    bindProviderOf(::ScanCaptureScreen)

    bindProviderOf(::ScanEditEnvironment)
    bindProviderOf(::ScanEditScreenModel)
    bindFactory { params: ScanEditParams -> ScanEditScreen(params) }
}
