package com.micrantha.eyespie.sharing

import platform.Foundation.NSURL

/** iOS-only extension that keeps the security-scoped URL below the platform boundary. */
interface IosExternalGameDocumentSource : ExternalGameDocumentSource {
    fun pendingDocumentUrl(): NSURL?
}
