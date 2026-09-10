package com.micrantha.eyespie.app

import com.micrantha.eyespie.sharing.IosExternalGameDocumentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import platform.Foundation.NSURL

internal object IosExternalIngress : IosExternalGameDocumentSource, ExternalAppIntentSource {
    private val pendingDocumentState = MutableStateFlow(false)
    private var pendingDocument: NSURL? = null

    private val pendingIntentState = MutableStateFlow<ExternalAppIntent?>(null)

    override val pending: StateFlow<Boolean> = pendingDocumentState.asStateFlow()
    override val intents: Flow<ExternalAppIntent> = pendingIntentState.filterNotNull()

    fun offerDocument(url: NSURL): Boolean {
        if (!url.isFileURL) return false
        if (!url.pathExtension.equals(EYESPIE_FILE_EXTENSION, ignoreCase = true)) return false
        if (pendingDocument != null) return false

        pendingDocument = url
        pendingDocumentState.value = true
        return true
    }

    fun offerDeepLink(
        scheme: String?,
        host: String?,
        pathSegments: List<String>,
        hasQuery: Boolean,
        hasFragment: Boolean,
        hasUserInfo: Boolean,
        hasPort: Boolean,
    ): Boolean {
        if (hasQuery || hasFragment || hasUserInfo || hasPort) return false
        val parsed = parseEyespieDeepLink(scheme, host, pathSegments) ?: return false
        pendingIntentState.value = parsed
        return true
    }

    override fun pendingDocumentUrl(): NSURL? = pendingDocument

    override fun acknowledgePendingDocument() {
        pendingDocument = null
        pendingDocumentState.value = false
    }

    override fun acknowledge(intent: ExternalAppIntent) {
        if (pendingIntentState.value == intent) {
            pendingIntentState.value = null
        }
    }
}

fun offerIosExternalDocument(url: NSURL): Boolean = IosExternalIngress.offerDocument(url)

fun offerIosDeepLink(
    scheme: String?,
    host: String?,
    pathSegments: List<String>,
    hasQuery: Boolean,
    hasFragment: Boolean,
    hasUserInfo: Boolean,
    hasPort: Boolean,
): Boolean = IosExternalIngress.offerDeepLink(
    scheme = scheme,
    host = host,
    pathSegments = pathSegments,
    hasQuery = hasQuery,
    hasFragment = hasFragment,
    hasUserInfo = hasUserInfo,
    hasPort = hasPort,
)

private const val EYESPIE_FILE_EXTENSION = "eyespie"
