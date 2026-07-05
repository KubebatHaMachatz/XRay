package com.demo.xray.engine.apk.axml

/**
 * Typed model of a decoded binary `AndroidManifest.xml`. Preserves namespaces, raw typed
 * attribute values, and resolved human-readable values where possible. See PRD FR-030.
 */
data class AxmlDocument(val root: AxmlElement)

data class AxmlElement(
    val namespaceUri: String?,
    val name: String,
    val attributes: List<AxmlAttribute>,
    val children: List<AxmlElement>,
    val lineNumber: Int,
) {
    fun attr(name: String, namespaceUri: String? = ANDROID_NAMESPACE): AxmlAttribute? =
        attributes.firstOrNull { it.name == name && (namespaceUri == null || it.namespaceUri == namespaceUri) }

    fun childrenNamed(name: String): List<AxmlElement> = children.filter { it.name == name }
}

/**
 * One decoded attribute. [rawValueString] holds the string-pool value when the raw value refers
 * to one (e.g. resource references), and is null otherwise. [resolvedValue] is always a
 * best-effort human-readable rendering suitable for display.
 */
data class AxmlAttribute(
    val namespaceUri: String?,
    val name: String,
    val valueType: Int,
    val intData: Int,
    val rawValueString: String?,
    val resolvedValue: String,
) {
    val asBoolean: Boolean?
        get() = if (valueType == AxmlValueType.TYPE_INT_BOOLEAN) intData != 0 else null

    val asInt: Int?
        get() =
            when (valueType) {
                AxmlValueType.TYPE_INT_DEC, AxmlValueType.TYPE_INT_HEX, AxmlValueType.TYPE_INT_BOOLEAN -> intData
                else -> null
            }

    val asString: String?
        get() = if (valueType == AxmlValueType.TYPE_STRING) rawValueString ?: resolvedValue else resolvedValue
}

const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

object AxmlValueType {
    const val TYPE_NULL = 0x00
    const val TYPE_REFERENCE = 0x01
    const val TYPE_ATTRIBUTE = 0x02
    const val TYPE_STRING = 0x03
    const val TYPE_FLOAT = 0x04
    const val TYPE_DIMENSION = 0x05
    const val TYPE_FRACTION = 0x06
    const val TYPE_DYNAMIC_REFERENCE = 0x07
    const val TYPE_INT_DEC = 0x10
    const val TYPE_INT_HEX = 0x11
    const val TYPE_INT_BOOLEAN = 0x12
    const val TYPE_INT_COLOR_ARGB8 = 0x1c
    const val TYPE_INT_COLOR_RGB8 = 0x1d
    const val TYPE_INT_COLOR_ARGB4 = 0x1e
    const val TYPE_INT_COLOR_RGB4 = 0x1f
}
