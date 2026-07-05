package com.demo.xray.engine.apk.axml

import java.io.ByteArrayOutputStream

/**
 * Minimal hand-written AXML *encoder*, used only by tests to build golden binary fixtures for
 * round-tripping [AxmlParser] without depending on the Android SDK build tools. Not part of the
 * production parser; production code never needs to write AXML.
 */
internal data class TestAttr(
    val namespaceUri: String?,
    val name: String,
    val valueType: Int,
    val data: Int = 0,
    val stringValue: String? = null,
)

internal data class TestElement(
    val namespaceUri: String?,
    val name: String,
    val attrs: List<TestAttr> = emptyList(),
    val children: List<TestElement> = emptyList(),
)

internal object AxmlEncoder {
    fun encode(root: TestElement): ByteArray {
        val strings = LinkedHashMap<String, Int>()
        fun intern(s: String?) {
            if (s != null) strings.getOrPut(s) { strings.size }
        }

        fun walk(e: TestElement) {
            intern(e.namespaceUri)
            intern(e.name)
            for (a in e.attrs) {
                intern(a.namespaceUri)
                intern(a.name)
                if (a.valueType == AxmlValueType.TYPE_STRING) intern(a.stringValue)
            }
            e.children.forEach(::walk)
        }
        walk(root)

        fun idx(s: String?): Int = if (s == null) -1 else strings.getValue(s)

        val stringPoolChunk = encodeStringPool(strings.keys.toList())
        val elementsBytes = ByteArrayOutputStream()

        fun writeStartElement(e: TestElement) {
            val chunkSize = 8 + 8 + 20 + e.attrs.size * 20
            writeU16(elementsBytes, ChunkType.RES_XML_START_ELEMENT_TYPE)
            writeU16(elementsBytes, 16)
            writeU32(elementsBytes, chunkSize)
            writeU32(elementsBytes, 0) // lineNumber
            writeU32(elementsBytes, -1) // comment
            writeU32(elementsBytes, idx(e.namespaceUri))
            writeU32(elementsBytes, idx(e.name))
            writeU16(elementsBytes, 20) // attributeStart
            writeU16(elementsBytes, 20) // attributeSize
            writeU16(elementsBytes, e.attrs.size)
            writeU16(elementsBytes, 0) // idIndex
            writeU16(elementsBytes, 0) // classIndex
            writeU16(elementsBytes, 0) // styleIndex
            for (a in e.attrs) {
                writeU32(elementsBytes, idx(a.namespaceUri))
                writeU32(elementsBytes, idx(a.name))
                val rawIdx = if (a.valueType == AxmlValueType.TYPE_STRING) idx(a.stringValue) else -1
                writeU32(elementsBytes, rawIdx)
                writeU16(elementsBytes, 8) // Res_value.size
                elementsBytes.write(0) // res0
                elementsBytes.write(a.valueType)
                val data = if (a.valueType == AxmlValueType.TYPE_STRING) idx(a.stringValue) else a.data
                writeU32(elementsBytes, data)
            }
        }

        fun writeEndElement(e: TestElement) {
            writeU16(elementsBytes, ChunkType.RES_XML_END_ELEMENT_TYPE)
            writeU16(elementsBytes, 16)
            writeU32(elementsBytes, 24)
            writeU32(elementsBytes, 0)
            writeU32(elementsBytes, -1)
            writeU32(elementsBytes, idx(e.namespaceUri))
            writeU32(elementsBytes, idx(e.name))
        }

        fun walkWrite(e: TestElement) {
            writeStartElement(e)
            e.children.forEach(::walkWrite)
            writeEndElement(e)
        }
        walkWrite(root)

        val body = elementsBytes.toByteArray()
        val totalChunkSize = 8 + stringPoolChunk.size + body.size
        val out = ByteArrayOutputStream()
        writeU16(out, ChunkType.RES_XML_TYPE)
        writeU16(out, 8)
        writeU32(out, totalChunkSize)
        out.write(stringPoolChunk)
        out.write(body)
        return out.toByteArray()
    }

    private fun encodeStringPool(values: List<String>): ByteArray {
        val flags = 1 shl 8 // UTF8_FLAG
        val stringData = ByteArrayOutputStream()
        val offsets = IntArray(values.size)
        for ((i, s) in values.withIndex()) {
            offsets[i] = stringData.size()
            val utf8 = s.toByteArray(Charsets.UTF_8)
            writeShortLen(stringData, s.length)
            writeShortLen(stringData, utf8.size)
            stringData.write(utf8)
            stringData.write(0)
        }

        val headerSize = 28
        val stringsStart = headerSize + values.size * 4
        val stringDataBytes = stringData.toByteArray()
        val chunkSize = stringsStart + stringDataBytes.size

        val out = ByteArrayOutputStream()
        writeU16(out, ChunkType.RES_STRING_POOL_TYPE)
        writeU16(out, headerSize)
        writeU32(out, chunkSize)
        writeU32(out, values.size)
        writeU32(out, 0) // styleCount
        writeU32(out, flags)
        writeU32(out, stringsStart)
        writeU32(out, 0) // stylesStart
        for (o in offsets) writeU32(out, o)
        out.write(stringDataBytes)
        return out.toByteArray()
    }

    private fun writeShortLen(out: ByteArrayOutputStream, len: Int) {
        check(len < 0x8000) { "test strings must be short" }
        if (len < 0x80) {
            out.write(len)
        } else {
            out.write(0x80 or (len shr 8))
            out.write(len and 0xFF)
        }
    }

    private fun writeU16(out: ByteArrayOutputStream, v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
    }

    private fun writeU32(out: ByteArrayOutputStream, v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
        out.write((v shr 16) and 0xFF)
        out.write((v shr 24) and 0xFF)
    }
}
