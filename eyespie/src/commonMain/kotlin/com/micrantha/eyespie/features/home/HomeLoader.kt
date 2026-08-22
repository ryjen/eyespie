package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.game.LocalGameResult

interface HomeLoader {
    suspend fun load(): LocalGameResult<HomeContent>
}
