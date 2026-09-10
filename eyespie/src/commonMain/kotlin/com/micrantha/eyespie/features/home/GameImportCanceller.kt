package com.micrantha.eyespie.features.home

interface GameImportCanceller {
    /** Lifecycle cancellation: clear transient bytes without committing external handoff consumption. */
    fun cancelImport()

    /** Explicit user/navigation discard: the current candidate will not be resumed. */
    fun discardImport() = cancelImport()
}
