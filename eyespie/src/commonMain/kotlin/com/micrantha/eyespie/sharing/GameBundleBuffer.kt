package com.micrantha.eyespie.sharing

import okio.Buffer

internal fun Buffer.writeLengthPrefixedUtf8(value: String) {
    val encoded = value.encodeToByteArray()
    writeInt(encoded.size)
    write(encoded)
}
