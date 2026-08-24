package com.micrantha.eyespie.game

interface GameSnapshotLoader {
    suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot>
}
