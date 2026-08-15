package com.micrantha.eyespie.persistence

object EmbeddingBlobCodec {
    fun encode(values: List<Float>): ByteArray {
        require(values.isNotEmpty()) { "embedding must not be empty" }
        val bytes = ByteArray(values.size * FLOAT_BYTES)
        values.forEachIndexed { index, value ->
            val bits = value.toRawBits()
            val offset = index * FLOAT_BYTES
            bytes[offset] = bits.toByte()
            bytes[offset + 1] = (bits ushr 8).toByte()
            bytes[offset + 2] = (bits ushr 16).toByte()
            bytes[offset + 3] = (bits ushr 24).toByte()
        }
        return bytes
    }

    fun decode(bytes: ByteArray): List<Float> {
        require(bytes.isNotEmpty()) { "embedding blob must not be empty" }
        require(bytes.size % FLOAT_BYTES == 0) { "embedding blob must contain complete Float values" }
        return List(bytes.size / FLOAT_BYTES) { index ->
            val offset = index * FLOAT_BYTES
            val bits = (bytes[offset].toInt() and 0xff) or
                ((bytes[offset + 1].toInt() and 0xff) shl 8) or
                ((bytes[offset + 2].toInt() and 0xff) shl 16) or
                ((bytes[offset + 3].toInt() and 0xff) shl 24)
            Float.fromBits(bits)
        }
    }

    private const val FLOAT_BYTES = 4
}
