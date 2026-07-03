package com.demo.xray.engine.apk.axml

import com.demo.xray.core.model.AnalysisError
import com.demo.xray.core.model.AnalysisOutcome

/**
 * Decodes a binary `AndroidManifest.xml` (Android Binary XML / AXML) into a typed [AxmlDocument].
 *
 * AXML format:
 *  - Compiled by `aapt2` / `aapt` at build time for efficiency (smaller, faster to parse)
 *  - Starts with a root RES_XML chunk containing a string pool and a sequence of chunks
 *  - Element chunks encode tag names and attributes; no closing tags are stored (tree structure
 *    is implicit in the nesting of start/end chunks)
 *
 * Design rationale (see ADR-0002):
 *  - Hand-written instead of using a third-party library (e.g., aXML, apktool's AXML parser)
 *    because existing libraries are often unmaintained or have large dependency footprints
 *  - Comprehensive tests (12+ including truncated/hostile input) ensure correctness
 *  - Every read goes through [ByteReader], which throws [AxmlParseException] rather than
 *    an unchecked exception on out-of-bounds access, so truncated/malicious input always
 *    yields a typed [AnalysisError.MalformedManifest] instead of crashing the app (PRD 15.4)
 *
 * Algorithm:
 *  1. Parse file header and validate root chunk type/size
 *  2. Iterate through chunks: build a string pool, parse element start/end chunks
 *  3. Maintain a depth-limited stack of open elements to build the tree
 *  4. Convert to typed [AxmlDocument] for downstream (test-friendly, framework-independent)
 */
object AxmlParser {
    private const val NO_INDEX = -1
    private const val MAX_DEPTH = 512          // Prevent stack-based DoS
    private const val MAX_ATTR_COUNT = 10_000  // Prevent huge attribute tables
    private const val MIN_NODE_HEADER_SIZE = 16
    private const val MIN_ATTR_EXT_SIZE = 20
    private const val MIN_ATTR_ENTRY_SIZE = 20

    fun parse(bytes: ByteArray): AnalysisOutcome<AxmlDocument> =
        try {
            AnalysisOutcome.Success(parseInternal(bytes))
        } catch (e: AxmlParseException) {
            AnalysisOutcome.Failure(AnalysisError.MalformedManifest(e.message))
        } catch (e: Exception) {
            // Defense in depth: no exception from untrusted-input parsing may propagate uncaught.
            AnalysisOutcome.Failure(AnalysisError.MalformedManifest(e::class.simpleName))
        }

    private class ElementFrame(
        val namespaceUri: String?,
        val name: String,
        val attributes: List<AxmlAttribute>,
        val children: MutableList<AxmlElement>,
        val lineNumber: Int,
    )

    private fun parseInternal(bytes: ByteArray): AxmlDocument {
        val reader = ByteReader(bytes)
        val fileType = reader.readU16()
        if (fileType != ChunkType.RES_XML_TYPE) {
            throw AxmlParseException("Not an AXML file (root chunk type=0x${fileType.toString(16)})")
        }
        val headerSize = reader.readU16()
        val chunkSize = reader.readU32AsInt()
        if (headerSize < 8 || chunkSize < headerSize || chunkSize > bytes.size) {
            throw AxmlParseException("Invalid root chunk size $chunkSize")
        }
        reader.seekTo(headerSize)

        var stringPool: StringPool? = null
        val stack = ArrayDeque<ElementFrame>()
        var root: AxmlElement? = null

        while (reader.remaining() >= 8 && reader.position < chunkSize) {
            val chunkStart = reader.position
            val type = reader.readU16()
            val chHeaderSize = reader.readU16()
            val chSize = reader.readU32AsInt()
            if (chHeaderSize < 8 || chSize < chHeaderSize || chunkStart.toLong() + chSize > bytes.size) {
                throw AxmlParseException("Invalid chunk at offset $chunkStart (size=$chSize)")
            }

            when (type) {
                ChunkType.RES_STRING_POOL_TYPE -> {
                    reader.seekTo(chunkStart)
                    stringPool = StringPool.parse(reader)
                    reader.seekTo(chunkStart + chSize)
                }

                ChunkType.RES_XML_RESOURCE_MAP_TYPE -> {
                    // Resource IDs are not required to resolve attribute name text (already
                    // string-pool-backed); the map is skipped rather than modeled.
                    reader.seekTo(chunkStart + chSize)
                }

                ChunkType.RES_XML_START_NAMESPACE_TYPE, ChunkType.RES_XML_END_NAMESPACE_TYPE -> {
                    reader.seekTo(chunkStart + chSize)
                }

                ChunkType.RES_XML_START_ELEMENT_TYPE -> {
                    val pool = stringPool ?: throw AxmlParseException("Element chunk before string pool")
                    if (chHeaderSize < MIN_NODE_HEADER_SIZE) {
                        throw AxmlParseException("Element node header too small ($chHeaderSize)")
                    }
                    reader.seekTo(chunkStart + 8)
                    val lineNumber = reader.readS32()
                    reader.skip(4) // comment index, unused

                    val attrExtStart = chunkStart + chHeaderSize
                    reader.seekTo(attrExtStart)
                    val nsIdx = reader.readS32()
                    val nameIdx = reader.readS32()
                    val attributeStart = reader.readU16()
                    val attributeSize = reader.readU16()
                    val attrCount = reader.readU16()
                    // idIndex, classIndex, styleIndex: not needed for manifest facts.

                    if (attributeSize < MIN_ATTR_ENTRY_SIZE) {
                        throw AxmlParseException("Invalid attribute entry size $attributeSize")
                    }
                    if (attrCount > MAX_ATTR_COUNT) {
                        throw AxmlParseException("Attribute count $attrCount exceeds safety limit")
                    }
                    if (attributeStart < MIN_ATTR_EXT_SIZE) {
                        throw AxmlParseException("Invalid attribute table offset $attributeStart")
                    }

                    val attrsBase = attrExtStart + attributeStart
                    val attrs = ArrayList<AxmlAttribute>(attrCount)
                    for (i in 0 until attrCount) {
                        reader.seekTo(attrsBase + i * attributeSize)
                        val attrNsIdx = reader.readS32()
                        val attrNameIdx = reader.readS32()
                        val attrRawValueIdx = reader.readS32()
                        reader.skip(2) // Res_value.size
                        reader.skip(1) // Res_value.res0
                        val dataType = reader.readU8()
                        val data = reader.readS32()
                        val name = pool.getOrNull(attrNameIdx) ?: "attr_0x${attrNameIdx.toString(16)}"
                        attrs +=
                            AxmlAttribute(
                                namespaceUri = resolveString(pool, attrNsIdx),
                                name = name,
                                valueType = dataType,
                                intData = data,
                                rawValueString = resolveString(pool, attrRawValueIdx),
                                resolvedValue = renderValue(pool, dataType, data),
                            )
                    }

                    if (stack.size >= MAX_DEPTH) {
                        throw AxmlParseException("Manifest XML nesting exceeds safe depth limit ($MAX_DEPTH)")
                    }

                    val elementName =
                        pool.getOrNull(nameIdx) ?: throw AxmlParseException("Element missing name string")

                    stack.addLast(
                        ElementFrame(
                            namespaceUri = resolveString(pool, nsIdx),
                            name = elementName,
                            attributes = attrs,
                            children = mutableListOf(),
                            lineNumber = lineNumber,
                        ),
                    )
                    reader.seekTo(chunkStart + chSize)
                }

                ChunkType.RES_XML_END_ELEMENT_TYPE -> {
                    val frame = stack.removeLastOrNull() ?: throw AxmlParseException("Unbalanced end-element chunk")
                    val element =
                        AxmlElement(
                            namespaceUri = frame.namespaceUri,
                            name = frame.name,
                            attributes = frame.attributes,
                            children = frame.children,
                            lineNumber = frame.lineNumber,
                        )
                    val parent = stack.lastOrNull()
                    if (parent != null) {
                        parent.children.add(element)
                    } else {
                        if (root != null) throw AxmlParseException("Multiple root elements")
                        root = element
                    }
                    reader.seekTo(chunkStart + chSize)
                }

                else -> {
                    // CDATA and any unrecognized/future chunk types are safely skipped.
                    reader.seekTo(chunkStart + chSize)
                }
            }
        }

        if (stack.isNotEmpty()) throw AxmlParseException("Truncated document: ${stack.size} unclosed element(s)")
        return AxmlDocument(root ?: throw AxmlParseException("No root element found"))
    }

    private fun resolveString(pool: StringPool, index: Int): String? =
        if (index == NO_INDEX) null else pool.getOrNull(index)

    private fun renderValue(pool: StringPool, dataType: Int, data: Int): String =
        when (dataType) {
            AxmlValueType.TYPE_NULL -> "null"
            AxmlValueType.TYPE_STRING -> pool.getOrNull(data) ?: "\"\""
            AxmlValueType.TYPE_REFERENCE -> if (data == 0) "@null" else "@0x%08x".format(data)
            AxmlValueType.TYPE_ATTRIBUTE -> "?0x%08x".format(data)
            AxmlValueType.TYPE_DYNAMIC_REFERENCE -> "@0x%08x".format(data)
            AxmlValueType.TYPE_FLOAT -> Float.fromBits(data).toString()
            AxmlValueType.TYPE_INT_DEC -> data.toString()
            AxmlValueType.TYPE_INT_HEX -> "0x%08x".format(data)
            AxmlValueType.TYPE_INT_BOOLEAN -> if (data != 0) "true" else "false"
            AxmlValueType.TYPE_INT_COLOR_ARGB8, AxmlValueType.TYPE_INT_COLOR_RGB8,
            AxmlValueType.TYPE_INT_COLOR_ARGB4, AxmlValueType.TYPE_INT_COLOR_RGB4,
            -> "#%08X".format(data)
            AxmlValueType.TYPE_DIMENSION, AxmlValueType.TYPE_FRACTION -> "0x%08x".format(data)
            else -> "0x%08x".format(data)
        }
}
