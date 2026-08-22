package com.micrantha.eyespie.sharing

data class DecodedGameBundle(
    val game: PortableGame,
    val unsignedBytes: ByteArray,
    val signature: ByteArray,
)
