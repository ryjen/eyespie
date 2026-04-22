package com.micrantha.eyespie.domain.entities

data class Session(
    val id: String,
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)
