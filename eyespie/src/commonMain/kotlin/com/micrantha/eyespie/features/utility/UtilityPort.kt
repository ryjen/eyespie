package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.game.LocalGameResult

interface UtilityPort {
    suspend fun loadUtility(): LocalGameResult<UtilityContent>
}
