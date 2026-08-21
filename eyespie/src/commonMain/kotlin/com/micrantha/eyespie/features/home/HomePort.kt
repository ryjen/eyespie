package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.game.LocalGameResult

interface HomePort {
    suspend fun load(): LocalGameResult<HomeContent>
}
