package com.demo.xray.engine.apk.axml

internal object ChunkType {
    const val RES_NULL_TYPE = 0x0000
    const val RES_STRING_POOL_TYPE = 0x0001
    const val RES_XML_TYPE = 0x0003

    const val RES_XML_START_NAMESPACE_TYPE = 0x0100
    const val RES_XML_END_NAMESPACE_TYPE = 0x0101
    const val RES_XML_START_ELEMENT_TYPE = 0x0102
    const val RES_XML_END_ELEMENT_TYPE = 0x0103
    const val RES_XML_CDATA_TYPE = 0x0104

    const val RES_XML_RESOURCE_MAP_TYPE = 0x0180
}
