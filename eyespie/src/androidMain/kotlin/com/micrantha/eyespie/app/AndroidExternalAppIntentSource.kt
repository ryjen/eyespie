package com.micrantha.eyespie.app

import android.content.Intent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class AndroidExternalAppIntentSource : ExternalAppIntentSource {
    private val channel = Channel<ExternalAppIntent>(capacity = Channel.CONFLATED)
    override val intents: Flow<ExternalAppIntent> = channel.receiveAsFlow()

    fun offer(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_VIEW) return false
        val uri = intent.data ?: return false
        if (uri.userInfo != null || uri.port != -1 || uri.query != null || uri.fragment != null) return false

        val parsed = parseEyespieDeepLink(
            scheme = uri.scheme,
            host = uri.host,
            pathSegments = uri.pathSegments,
        ) ?: return false
        return channel.trySend(parsed).isSuccess
    }
}
