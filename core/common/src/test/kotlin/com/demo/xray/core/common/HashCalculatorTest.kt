package com.demo.xray.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class HashCalculatorTest {
    @Test
    fun `known vector - empty input`() {
        val acc = Sha256HashCalculator.newSha256Accumulator()
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            acc.digestHex(),
        )
    }

    @Test
    fun `known vector - abc`() {
        val acc = Sha256HashCalculator.newSha256Accumulator()
        val bytes = "abc".toByteArray(Charsets.US_ASCII)
        acc.update(bytes, 0, bytes.size)
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            acc.digestHex(),
        )
    }

    @Test
    fun `incremental updates match single update`() {
        val whole = Sha256HashCalculator.newSha256Accumulator()
        val wholeBytes = "hello world".toByteArray(Charsets.UTF_8)
        whole.update(wholeBytes, 0, wholeBytes.size)

        val chunked = Sha256HashCalculator.newSha256Accumulator()
        chunked.update("hello".toByteArray(Charsets.UTF_8), 0, 5)
        chunked.update(" world".toByteArray(Charsets.UTF_8), 0, 6)

        assertEquals(whole.digestHex(), chunked.digestHex())
    }
}
