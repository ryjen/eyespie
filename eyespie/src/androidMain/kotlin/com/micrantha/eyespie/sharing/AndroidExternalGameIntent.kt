package com.micrantha.eyespie.sharing

import android.content.Intent
import android.net.Uri

/**
 * Resolve a narrowly scoped external Eyespie document handoff.
 *
 * This is routing only, never authority: the returned URI is still untrusted and must pass the
 * normal bounded bundle parser/signature/domain validation before preview or persistence.
 */
fun externalEyespieDocumentUri(intent: Intent?): Uri? {
    intent ?: return null
    val uri = when (intent.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> intent.clipData
            ?.takeIf { it.itemCount == 1 }
            ?.getItemAt(0)
            ?.uri
            ?: legacyStreamUri(intent)
        else -> null
    } ?: return null

    if (uri.scheme != "content" && uri.scheme != "file") return null

    val declaredEyespieType = intent.type.equals(EYESPIE_ANDROID_MIME_TYPE, ignoreCase = true)
    val eyespieExtension = uri.lastPathSegment
        ?.substringBefore('?')
        ?.endsWith(".eyespie", ignoreCase = true)
        == true

    return uri.takeIf { declaredEyespieType || eyespieExtension }
}

@Suppress("DEPRECATION")
private fun legacyStreamUri(intent: Intent): Uri? =
    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
