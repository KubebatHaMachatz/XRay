package com.demo.xray.engine.apk.axml

/**
 * Decoded `RES_STRING_POOL_TYPE` chunk (ARSC/AXML shared format). Strings are decoded lazily on
 * first access and cached, since manifests can declare pools with thousands of entries but a
 * given manifest typically touches only a fraction of them.
 */
internal class StringPool(
    private val chunkBytes: ByteArray,
    private val stringsStart: Int,
    private val stringOffsets: IntArray,
    private val isUtf8: Boolean,
) {
    private val cache = arrayOfNulls<String>(stringOffsets.size)

    val size: Int get() = stringOffsets.size

    fun getOrNull(index: Int): String? {
        if (index < 0 || index >= stringOffsets.size) return null
        cache[index]?.let { return it }
        val decoded = decodeAt(stringsStart + stringOffsets[index])
        cache[index] = decoded
        return decoded
    }

    private fun decodeAt(offset: Int): String {
        val reader = ByteReader(chunkBytes)
        reader.seekTo(offset)
        return if (isUtf8) {
            // utf16 length (unused for decoding, only for pre-sizing), then utf8 byte length.
            readUtf8Length(reader)
            val byteLen = readUtf8Length(reader)
            val bytes = reader.readBytes(byteLen)
            String(bytes, Charsets.UTF_8)
        } else {
            val charLen = readUtf16Length(reader)
            val bytes = reader.readBytes(charLen * 2)
            String(bytes, Charsets.UTF_16LE)
        }
    }

    /** Length encoding shared by UTF-8 and UTF-16 string entries: 1 or 2 units, high bit = continuation. */
    private fun readUtf8Length(reader: ByteReader): Int {
        val first = reader.readU8()
        return if (first and 0x80 != 0) {
            val second = reader.readU8()
            ((first and 0x7F) shl 8) or second
        } else {
            first
        }
    }

    private fun readUtf16Length(reader: ByteReader): Int {
        val first = reader.readU16()
        return if (first and 0x8000 != 0) {
            val second = reader.readU16()
            ((first and 0x7FFF) shl 16) or second
        } else {
            first
        }
    }

    companion object {
        private const val UTF8_FLAG = 1 shl 8

        /** [reader] must be positioned at the start of the chunk header (type field). */
        fun parse(reader: ByteReader): StringPool {
            val chunkStart = reader.position
            val type = reader.readU16()
            if (type != ChunkType.RES_STRING_POOL_TYPE) {
                throw AxmlParseException("Expected string pool chunk, found 0x${type.toString(16)}")
            }
            val headerSize = reader.readU16()
            val chunkSize = reader.readU32AsInt()
            if (headerSize < MIN_HEADER_SIZE || chunkSize < headerSize || chunkStart.toLong() + chunkSize > reader.size) {
                throw AxmlParseException("Invalid string pool chunk size $chunkSize")
            }

            val stringCount = reader.readU32AsInt()
            val styleCount = reader.readU32AsInt()
            val flags = reader.readU32AsInt()
            val stringsStart = reader.readU32AsInt()
            @Suppress("UNUSED_VARIABLE") val stylesStart = reader.readU32AsInt()

            if (stringCount < 0 || stringCount > MAX_STRING_COUNT) {
                throw AxmlParseException("Unreasonable string pool count $stringCount")
            }

            // Defensive: skip any header bytes beyond the standard 28-byte layout we just read,
            // in case a future/nonstandard producer declares a larger headerSize.
            reader.seekTo(chunkStart + headerSize)

            val offsets = IntArray(stringCount)
            for (i in 0 until stringCount) offsets[i] = reader.readU32AsInt()
            // Style offsets are not needed for manifest decoding; skip them.
            if (styleCount > 0) reader.skip(styleCount * 4)

            // The whole chunk's bytes, with offsets relative to chunkStart, so string decoding can
            // re-seek independently of the outer reader's cursor.
            val absoluteStringsStart = chunkStart + stringsStart
            reader.seekTo(chunkStart)
            val chunkBytes = reader.readBytes(chunkSize)
            reader.seekTo(chunkStart + chunkSize)

            return StringPool(
                chunkBytes = chunkBytes,
                stringsStart = absoluteStringsStart - chunkStart,
                stringOffsets = offsets,
                isUtf8 = (flags and UTF8_FLAG) != 0,
            )
        }

        private const val MAX_STRING_COUNT = 5_000_000
        private const val MIN_HEADER_SIZE = 28
    }
}
