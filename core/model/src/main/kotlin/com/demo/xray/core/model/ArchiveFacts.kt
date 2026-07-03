package com.demo.xray.core.model

enum class ArchiveEntryCategory {
    DEX,
    NATIVE_LIBRARY,
    RESOURCES,
    COMPILED_MANIFEST,
    ASSETS,
    SIGNING_METADATA,
    KOTLIN_METADATA,
    OTHER,
}

data class ArchiveEntryFact(
    val path: String,
    val isDirectory: Boolean,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val category: ArchiveEntryCategory,
    val isDuplicate: Boolean = false,
)

data class ArchiveInventoryFact(
    val entries: List<ArchiveEntryFact>,
    val totalCompressedSize: Long,
    val totalUncompressedSize: Long,
    val duplicateCriticalEntryNames: List<String> = emptyList(),
    val expansionRatioExceedsPolicy: Boolean = false,
    val warnings: List<String> = emptyList(),
)
