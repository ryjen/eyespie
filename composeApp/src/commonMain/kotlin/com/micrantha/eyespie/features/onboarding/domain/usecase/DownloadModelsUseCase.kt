package com.micrantha.eyespie.features.onboarding.domain.usecase

import com.benasher44.uuid.uuid4
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.domain.usecase.useCase
import com.micrantha.bluebell.platform.BackgroundDownloader
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.domain.entities.UrlFile
import eyespie.composeapp.generated.resources.notification_title_download_model

class DownloadModelUseCase(
    private val i18n: LocalizedRepository,
    private val downloader: BackgroundDownloader,
) {
    operator fun invoke(name: String, download: UrlFile): Result<Unit> =
        useCase {
            val tag = uuid4().toString()
            downloader.startDownload(
                tag = tag,
                url = download,
                name = i18n.string(S.notification_title_download_model, name),
            )
        }
}
