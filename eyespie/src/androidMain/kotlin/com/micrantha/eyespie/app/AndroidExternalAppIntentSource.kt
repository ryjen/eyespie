package com.micrantha.eyespie.app

import android.content.Intent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class AndroidExternalAppIntentSource : ExternalAppIntentSource {
    private val channel = Channel<ExternalAppIntent>(capacity = Channel.CONFLATED)
    override val intents: Flow<ExternalAppIntent> = channel.receiveAsFlow()
    private var pendingIntent: ExternalAppIntent? = null

    fun offer(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_VIEW) return false
        val uri = intent.data ?: return false
        if (uri.userInfo != null || uri.port != -1 || uri.query != null || uri.fragment != null) return false

        val parsed = parseEyespieDeepLink(
            scheme = uri.scheme,
            host = uri.host,
            pathSegments = uri.pathSegments,
        ) ?: return false
        return enqueue(parsed)
    }

    /** Restore only a previously parsed canonical local-game id from Android saved state. */
    fun restorePendingGameId(gameId: String): Boolean {
        val parsed = parseEyespieDeepLink(
            scheme = "eyespie",
            host = "game",
            pathSegments = listOf(gameId),
        ) ?: return false
        return enqueue(parsed)
    }

    fun pendingGameIdState(): String? =
        (pendingIntent as? ExternalAppIntent.OpenLocalGame)?.gameId?.value

    override fun acknowledge(intent: ExternalAppIntent) {
        if (pendingIntent == intent) {
            pendingIntent = null
        }
    }

    private fun enqueue(intent: ExternalAppIntent): Boolean {
        pendingIntent = intent
        val result = channel.trySend(intent)
        if (result.isFailure && pendingIntent == intent) {
            pendingIntent = null
        }
        return result.isSuccess
    }
}
