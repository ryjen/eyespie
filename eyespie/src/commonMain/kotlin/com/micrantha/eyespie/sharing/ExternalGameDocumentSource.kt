package com.micrantha.eyespie.sharing

import kotlinx.coroutines.flow.StateFlow

/**
 * One-shot external document ingress owned by the platform shell.
 *
 * The platform may receive an Eyespie document through a file association, attachment, or other
 * OS-owned handoff. URI/path/platform objects stay below this boundary. [pending] only signals that
 * a bounded document is waiting to be consumed through the normal [GameDocumentTransfer.read]
 * path, preserving the existing verified import-preview authority boundary.
 */
interface ExternalGameDocumentSource {
    val pending: StateFlow<Boolean>
}
