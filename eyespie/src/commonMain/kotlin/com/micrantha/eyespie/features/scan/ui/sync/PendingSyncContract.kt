package com.micrantha.eyespie.features.scan.ui.sync

import com.micrantha.eyespie.data.PendingCapture

data class PendingSyncState(
    val pending: List<PendingCapture> = emptyList()
)

sealed interface PendingSyncAction {
    data object Load : PendingSyncAction
    data class Loaded(val pending: List<PendingCapture>) : PendingSyncAction
    data class Delete(val id: String) : PendingSyncAction
    data object Back : PendingSyncAction
}
