package com.micrantha.eyespie.features.onboarding.usecase

import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.domain.usecase.useCase
import com.micrantha.bluebell.platform.BackgroundDownloader

class DownloadModelsUseCase(
    private val i18n: LocalizedRepository,
    private val downloader: BackgroundDownloader,
) {
    operator fun invoke(): Result<Unit> = useCase {
        /*aiRepository.models.forEach { modelInfo ->
            val tag = uuid4().toString()
            downloader.startDownload(
                tag = tag,
                url = modelInfo.model,
                name = i18n.string(S.notification_title_download_model, modelInfo.name),
            )
            downloader.startDownload(
                tag = tag,
                url = modelInfo.encoder,
                name = i18n.string(S.notification_title_download_encoder, modelInfo.name)
            )
        }*/
    }
}
