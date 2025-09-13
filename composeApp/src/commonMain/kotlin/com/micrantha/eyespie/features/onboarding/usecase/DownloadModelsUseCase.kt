package com.micrantha.eyespie.features.onboarding.usecase

import com.benasher44.uuid.uuid4
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.domain.usecase.useCase
import com.micrantha.bluebell.platform.BackgroundDownloader
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.domain.repository.AiRepository
import eyespie.composeapp.generated.resources.notification_title_download_encoder
import eyespie.composeapp.generated.resources.notification_title_download_model

class DownloadModelsUseCase(
    private val i18n: LocalizedRepository,
    private val aiRepository: AiRepository,
    private val downloader: BackgroundDownloader,
) {
    operator fun invoke(): Result<Unit> = useCase {
        aiRepository.models.forEach { modelInfo ->
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
        }
    }
}
