package com.micrantha.eyespie.model

import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayAssetDeliveryStateMapperTest {
    @Test
    fun waitingForWifiMapsToQueuedReason() {
        val state = assetPackState(status = AssetPackStatus.WAITING_FOR_WIFI)

        assertEquals(
            ModelAssetState.Queued(QueueReason.WaitingForWifi),
            PlayAssetDeliveryStateMapper.map(state),
        )
    }

    @Test
    fun confirmationRequiredMapsToQueuedReason() {
        val state = assetPackState(status = AssetPackStatus.REQUIRES_USER_CONFIRMATION)

        assertEquals(
            ModelAssetState.Queued(QueueReason.WaitingForPlatformConfirmation),
            PlayAssetDeliveryStateMapper.map(state),
        )
    }

    @Test
    fun downloadingPreservesByteProgress() {
        val state = assetPackState(
            status = AssetPackStatus.DOWNLOADING,
            downloadedBytes = 128L,
            totalBytes = 512L,
        )

        assertEquals(
            ModelAssetState.Downloading(downloadedBytes = 128L, totalBytes = 512L),
            PlayAssetDeliveryStateMapper.map(state),
        )
    }

    @Test
    fun completedBeginsVerificationInsteadOfBecomingReady() {
        val state = assetPackState(
            status = AssetPackStatus.COMPLETED,
            totalBytes = 512L,
        )

        assertEquals(
            ModelAssetState.Verifying(verifiedBytes = 0L, totalBytes = 512L),
            PlayAssetDeliveryStateMapper.map(state),
        )
    }

    @Test
    fun networkFailureIsRecoverableAndStable() {
        val state = assetPackState(
            status = AssetPackStatus.FAILED,
            errorCode = AssetPackErrorCode.NETWORK_ERROR,
        )

        assertEquals(
            ModelAssetState.Failed(
                stage = FailureStage.Download,
                recoverable = true,
                diagnosticCode = "pad.network_error",
            ),
            PlayAssetDeliveryStateMapper.map(state),
        )
    }

    @Test
    fun accessDeniedFailureIsTerminal() {
        val state = assetPackState(
            status = AssetPackStatus.FAILED,
            errorCode = AssetPackErrorCode.ACCESS_DENIED,
        )

        assertEquals(
            ModelAssetState.Failed(
                stage = FailureStage.Download,
                recoverable = false,
                diagnosticCode = "pad.access_denied",
            ),
            PlayAssetDeliveryStateMapper.map(state),
        )
    }

    private fun assetPackState(
        status: Int,
        errorCode: Int = AssetPackErrorCode.NO_ERROR,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L,
    ): AssetPackState = mockk {
        every { status() } returns status
        every { errorCode() } returns errorCode
        every { bytesDownloaded() } returns downloadedBytes
        every { totalBytesToDownload() } returns totalBytes
    }
}
