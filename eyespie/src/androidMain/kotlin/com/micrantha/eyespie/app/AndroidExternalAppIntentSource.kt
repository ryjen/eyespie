package com.micrantha.eyespie.app

import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class AndroidExternalAppIntentSource : ExternalAppIntentSource {
    private val pendingState = MutableStateFlow<ExternalAppIntent?>(null)
    override val intents: Flow<ExternalAppIntent> = pendingState.filterNotNull()

    fun offer(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_VIEW) return false
        val uri = intent.data ?: return false
        if (uri.userInfo != null || uri.port != -1 || uri.query != null || uri.fragment != null) return false

        val parsed = parseEyespieDeepLink(
            scheme = uri.scheme,
            host = uri.host,
            pathSegments = uri.pathSegments,
        ) ?: return false
        pendingState.value = parsed
        return true
    }

    /** Restore only a previously parsed canonical local-game id from Android saved state. */
    fun restorePendingGameId(gameId: String): Boolean {
        val parsed = parseEyespieDeepLink(
            scheme = "eyespie",
            host = "game",
            pathSegments = listOf(gameId),
        ) ?: return false
        pendingState.value = parsed
        return true
    }

    fun pendingGameIdState(): String? =
        (pendingState.value as? ExternalAppIntent.OpenLocalGame)?.gameId?.value

    override fun acknowledge(intent: ExternalAppIntent) {
        if (pendingState.value == intent) {
            pendingState.value = null
        }
    }
}
