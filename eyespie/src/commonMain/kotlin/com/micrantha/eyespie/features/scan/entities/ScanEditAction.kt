package com.micrantha.eyespie.features.scan.entities

import com.micrantha.eyespie.domain.entities.AiProof

sealed interface ScanEditAction {
    data class Init(val params: ScanEditParams) : ScanEditAction

    data object SaveScanEdit : ScanEditAction

    data object SaveThingError : ScanEditAction

    data object LoadError : ScanEditAction

    data object Retry : ScanEditAction

    data class SelectClue(val id: Int) : ScanEditAction

    data class AnalyzedClues(val value: AiProof) : ScanEditAction
}
