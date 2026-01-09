package com.micrantha.bluebell.observability.domain

data class SessionInfo(
    val userId: String? = null,
    val sessionId: String? = null,
)

fun interface SessionInfoProvider {
    fun get(): SessionInfo
}
