package com.micrantha.eyespie.features.scan.entities

data class ScanClue(
    val id: Int,
    val answer: String,
    val clue: String,
    val isSelected: Boolean,
)
