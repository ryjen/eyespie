package com.micrantha.eyespie.sharing

import kotlinx.coroutines.flow.StateFlow

/**
 * One-shot external document ingress owned by the platform shell.
 *
 * The platform may receive an Eyespie document through a file association, attachment, or other
 * OS-owned handoff. URI/path/platform objects stay below this boundary. [pending] only signals that
 * a bounded document is waiting to be consumed through the normal [GameDocumentTransfer.read]
 * path, preserving the existing verified import-preview authority boundary.
 *
 * A pending handoff remains retryable until common import preparation has reached a stable preview
 * or terminal result. [acknowledgePendingDocument] commits that consumption only after that point;
 * cancellation before then must leave the platform-owned handoff pending.
 */
interface ExternalGameDocumentSource {
    val pending: StateFlow<Boolean>

    fun acknowledgePendingDocument()
}
