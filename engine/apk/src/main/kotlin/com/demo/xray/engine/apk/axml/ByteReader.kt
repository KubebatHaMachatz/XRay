package com.demo.xray.engine.apk.axml

/**
 * Thrown internally for any malformed/truncated chunk; always caught at the parser boundary.
 *
 * This is used instead of unchecked exceptions to make error handling explicit
 * and to ensure malformed APKs never cause a crash (per PRD principle: safety-first parsing).
 */
class AxmlParseException(message: String) : Exception(message)

/**
 * Bounds-checked little-endian cursor over a byte array. Every read is guarded against
 * buffer overrun and throws [AxmlParseException] on any violation.
 *
 * Design principle: malformed AXML data (truncated chunks, out-of-order offsets, etc.)
 * is treated as a failed analysis step, not as a crash. This is critical since we are
 * analyzing untrusted APK files that may be corrupted, malicious, or misformatted.
 *
 * See PRD 15.4 (safety limits) for the threat model.
 *
 * All reads are little-endian (Intel byte order), which is the standard for Android.
 */
internal class ByteReader(private val data: ByteArray) {
    var position: Int = 0
        private set

    val size: Int get() = data.size

    /**
     * Seek to an absolute position in the data.
     * Throws if the position is out of range (outside [0, size]).
     */
    fun seekTo(pos: Int) {
        require(pos in 0..data.size) { "seek out of range" }
        position = pos
    }

    /** How many unread bytes remain from the current position. */
    fun remaining(): Int = data.size - position

    /**
     * Guard clause for all reads: ensures at least [minBytes] bytes are available.
     * Throws [AxmlParseException] if the read would exceed bounds.
     *
     * Note: we check position.toLong() to catch integer overflow attacks where
     * position + minBytes could wrap around in a 32-bit int.
     */
    private fun require(minBytes: Int) {
        if (position < 0 || minBytes < 0 || position.toLong() + minBytes > data.size) {
            throw AxmlParseException("Unexpected end of data at offset $position (need $minBytes bytes, size=${data.size})")
        }
    }

    fun readU8(): Int {
        require(1)
        val v = data[position].toInt() and 0xFF
        position += 1
        return v
    }

    fun readU16(): Int {
        require(2)
        val b0 = data[position].toInt() and 0xFF
        val b1 = data[position + 1].toInt() and 0xFF
        position += 2
        return b0 or (b1 shl 8)
    }

    fun readS32(): Int {
        require(4)
        val b0 = data[position].toInt() and 0xFF
        val b1 = data[position + 1].toInt() and 0xFF
        val b2 = data[position + 2].toInt() and 0xFF
        val b3 = data[position + 3].toInt() and 0xFF
        position += 4
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    /** Same bit pattern as [readS32]; named for call-site clarity when a value is size/offset-like. */
    fun readU32AsInt(): Int = readS32()

    fun readBytes(count: Int): ByteArray {
        require(count)
        val out = data.copyOfRange(position, position + count)
        position += count
        return out
    }

    fun skip(count: Int) {
        require(count)
        position += count
    }
}
