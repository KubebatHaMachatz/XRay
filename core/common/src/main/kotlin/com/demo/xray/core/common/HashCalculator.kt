package com.demo.xray.core.common

import java.security.MessageDigest

/** Incremental hash calculation so callers never need to buffer a whole file. */
interface HashCalculator {
    fun newSha256Accumulator(): Sha256Accumulator
}

interface Sha256Accumulator {
    fun update(buffer: ByteArray, offset: Int, length: Int)

    /** Lowercase hex digest. Terminal operation; the accumulator must not be reused after this. */
    fun digestHex(): String
}

object Sha256HashCalculator : HashCalculator {
    override fun newSha256Accumulator(): Sha256Accumulator = MessageDigestAccumulator(MessageDigest.getInstance("SHA-256"))

    private class MessageDigestAccumulator(private val digest: MessageDigest) : Sha256Accumulator {
        override fun update(buffer: ByteArray, offset: Int, length: Int) {
            digest.update(buffer, offset, length)
        }

        override fun digestHex(): String = digest.digest().joinToString("") { "%02x".format(it) }
    }
}
