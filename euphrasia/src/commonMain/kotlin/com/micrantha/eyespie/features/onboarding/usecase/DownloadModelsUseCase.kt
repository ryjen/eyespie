package com.micrantha.eyespie.features.onboarding.usecase

import com.benasher44.uuid.uuid4
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.domain.usecase.useCase
import com.micrantha.bluebell.platform.BackgroundDownloader
import com.micrantha.bluebell.plugin.BluebellAssetConfig
import com.micrantha.eyespie.app.S
import eyespie.euphrasia.generated.resources.notification_title_download_model

class DownloadModelUseCase(
    private val i18n: LocalizedRepository,
    private val downloader: BackgroundDownloader,
) {
    operator fun invoke(name: String, download: BluebellAssetConfig.Download): Result<Unit> =
        useCase {
            val tag = uuid4().toString()
            downloader.startDownload(
                tag = tag,
                url = download.url,
                checksum = download.checksum,
                name = i18n.string(S.notification_title_download_model, name),
            )
        }
}
