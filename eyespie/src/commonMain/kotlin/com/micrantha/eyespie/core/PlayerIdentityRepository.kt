package com.micrantha.eyespie.core

interface PlayerIdentityRepository {
    suspend fun current(): PlayerIdentity
}
