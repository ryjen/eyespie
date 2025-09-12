package com.micrantha.eyespie.features.onboarding.usecase

import com.benasher44.uuid.uuid4
import com.micrantha.bluebell.domain.security.hash
import com.micrantha.bluebell.platform.BackgroundDownloader
import com.micrantha.eyespie.domain.repository.AiRepository

class DownloadModelsUseCase(
    private val aiRepository: AiRepository,
    private val downloader: BackgroundDownloader,
) {
    operator fun invoke() {
        val tag = uuid4().toString()
        aiRepository.models.forEach { modelInfo ->
            downloader.startDownload(
                tag = tag,
                url = modelInfo.url,
                name = modelInfo.name,
                fileName = hash(modelInfo.name),
                checksum = modelInfo.checksum
            )
        }
    }
}
