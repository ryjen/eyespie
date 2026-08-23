package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.game.LocalGameResult

interface UtilityLoader {
    suspend fun loadUtility(): LocalGameResult<UtilityContent>
}
