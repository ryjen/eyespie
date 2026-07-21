package com.micrantha.eyespie.model

import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus

internal object PlayAssetDeliveryStateMapper {
    fun map(state: AssetPackState): ModelAssetState = when (state.status()) {
        AssetPackStatus.NOT_INSTALLED,
        AssetPackStatus.UNKNOWN,
        -> ModelAssetState.NotInstalled

        AssetPackStatus.PENDING -> ModelAssetState.Queued()
        AssetPackStatus.WAITING_FOR_WIFI -> ModelAssetState.Queued(QueueReason.WaitingForWifi)
        AssetPackStatus.REQUIRES_USER_CONFIRMATION ->
            ModelAssetState.Queued(QueueReason.WaitingForPlatformConfirmation)

        AssetPackStatus.DOWNLOADING,
        AssetPackStatus.TRANSFERRING,
        -> ModelAssetState.Downloading(
            downloadedBytes = state.bytesDownloaded().coerceAtLeast(0L),
            totalBytes = state.totalBytesToDownload().takeIf { it > 0L },
        )

        AssetPackStatus.COMPLETED -> ModelAssetState.Verifying(
            verifiedBytes = 0L,
            totalBytes = state.totalBytesToDownload().coerceAtLeast(0L),
        )

        AssetPackStatus.CANCELED -> ModelAssetState.NotInstalled
        AssetPackStatus.FAILED -> ModelAssetState.Failed(
            stage = FailureStage.Download,
            recoverable = isRecoverable(state.errorCode()),
            diagnosticCode = diagnosticCode(state.errorCode()),
        )

        else -> ModelAssetState.Failed(
            stage = FailureStage.Download,
            recoverable = false,
            diagnosticCode = "pad.unknown_status.${state.status()}",
        )
    }

    private fun isRecoverable(errorCode: Int): Boolean = when (errorCode) {
        AssetPackErrorCode.NETWORK_ERROR,
        AssetPackErrorCode.INSUFFICIENT_STORAGE,
        AssetPackErrorCode.INTERNAL_ERROR,
        AssetPackErrorCode.DOWNLOAD_NOT_FOUND,
        -> true

        else -> false
    }

    private fun diagnosticCode(errorCode: Int): String = when (errorCode) {
        AssetPackErrorCode.NO_ERROR -> "pad.no_error"
        AssetPackErrorCode.APP_UNAVAILABLE -> "pad.app_unavailable"
        AssetPackErrorCode.PACK_UNAVAILABLE -> "pad.pack_unavailable"
        AssetPackErrorCode.INVALID_REQUEST -> "pad.invalid_request"
        AssetPackErrorCode.DOWNLOAD_NOT_FOUND -> "pad.download_not_found"
        AssetPackErrorCode.API_NOT_AVAILABLE -> "pad.api_not_available"
        AssetPackErrorCode.NETWORK_ERROR -> "pad.network_error"
        AssetPackErrorCode.ACCESS_DENIED -> "pad.access_denied"
        AssetPackErrorCode.INSUFFICIENT_STORAGE -> "pad.insufficient_storage"
        AssetPackErrorCode.APP_NOT_OWNED -> "pad.app_not_owned"
        AssetPackErrorCode.INTERNAL_ERROR -> "pad.internal_error"
        else -> "pad.error.$errorCode"
    }
}
