package com.micrantha.eyespie.sharing

internal class ByteCursor(private val bytes: ByteArray) {
    var position: Int = 0
        private set

    fun exhausted(): Boolean = position == bytes.size

    fun expectMagic() {
        val actual = readBytes(GAME_BUNDLE_MAGIC.size)
        if (!actual.contentEquals(GAME_BUNDLE_MAGIC)) failGameBundle(GameBundleFailureCode.BAD_MAGIC)
    }

    fun requireInt(expected: Int, code: GameBundleFailureCode) {
        if (readInt() != expected) failGameBundle(code)
    }

    fun readString(maxBytes: Int): String {
        val length = readInt()
        if (length < 0 || length > maxBytes) failGameBundle(GameBundleFailureCode.INVALID_LENGTH)
        val encoded = readBytes(length)
        return try {
            encoded.decodeToString(throwOnInvalidSequence = true)
        } catch (_: Exception) {
            failGameBundle(GameBundleFailureCode.INVALID_UTF8)
        }
    }

    fun readInt(): Int {
        requireRemaining(4)
        val result =
            (unsigned(bytes[position]) shl 24) or
                (unsigned(bytes[position + 1]) shl 16) or
                (unsigned(bytes[position + 2]) shl 8) or
                unsigned(bytes[position + 3])
        position += 4
        return result
    }

    fun readLong(): Long {
        requireRemaining(8)
        var result = 0L
        repeat(8) {
            result = (result shl 8) or unsigned(bytes[position + it]).toLong()
        }
        position += 8
        return result
    }

    fun readBytes(length: Int): ByteArray {
        if (length < 0) failGameBundle(GameBundleFailureCode.INVALID_LENGTH)
        requireRemaining(length)
        val result = bytes.copyOfRange(position, position + length)
        position += length
        return result
    }

    private fun requireRemaining(length: Int) {
        if (length > bytes.size - position) failGameBundle(GameBundleFailureCode.TRUNCATED)
    }

    private fun unsigned(value: Byte): Int = value.toInt() and 0xff
}
