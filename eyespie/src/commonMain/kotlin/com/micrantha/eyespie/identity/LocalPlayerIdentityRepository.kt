package com.micrantha.eyespie.identity

import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository

class LocalPlayerIdentityRepository(
    private val signingIdentity: SigningIdentity = PlatformSigningIdentity(),
    private val displayName: String = DEFAULT_DISPLAY_NAME,
) : PlayerIdentityRepository {
    init {
        require(displayName.isNotBlank()) { "display name must not be blank" }
    }

    override suspend fun current(): PlayerIdentity = PlayerIdentity(
        id = playerIdFor(signingIdentity.publicKey()),
        displayName = displayName,
    )

    companion object {
        const val DEFAULT_DISPLAY_NAME = "Agent"
    }
}
