package com.demo.xray.engine.apk

import com.demo.xray.core.common.AnalysisLimits
import com.demo.xray.core.model.AnalysisError
import com.demo.xray.core.model.AnalysisOutcome
import com.demo.xray.core.model.ArchiveEntryCategory
import com.demo.xray.core.model.ArchiveEntryFact
import com.demo.xray.core.model.ArchiveInventoryFact
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

const val ANDROID_MANIFEST_ENTRY_NAME = "AndroidManifest.xml"

/** Critical entry names/prefixes that must not appear more than once. See PRD FR-005. */
private val CRITICAL_SINGLETON_NAMES = setOf(ANDROID_MANIFEST_ENTRY_NAME)

/**
 * Validates ZIP/APK structure without ever extracting the archive tree to the filesystem. See
 * PRD FR-005 and 15.3.
 */
interface ApkValidator {
    fun validate(apkFile: File): AnalysisOutcome<ArchiveInventoryFact>
}

class DefaultApkValidator : ApkValidator {
    override fun validate(apkFile: File): AnalysisOutcome<ArchiveInventoryFact> {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return AnalysisOutcome.Failure(AnalysisError.UnreadableSource("missing or empty file"))
        }

        val zipFile =
            try {
                ZipFile(apkFile)
            } catch (e: ZipException) {
                return AnalysisOutcome.Failure(AnalysisError.InvalidZip(e::class.simpleName))
            } catch (e: IllegalArgumentException) {
                // Thrown by java.util.zip for a corrupt/negative central-directory offset.
                return AnalysisOutcome.Failure(AnalysisError.InvalidZip(e::class.simpleName))
            }

        return zipFile.use { zip ->
            val rawEntries: List<ZipEntry> =
                try {
                    zip.entries().asSequence().toList()
                } catch (e: ZipException) {
                    return AnalysisOutcome.Failure(AnalysisError.InvalidZip(e::class.simpleName))
                }

            if (rawEntries.size > AnalysisLimits.MAX_ZIP_ENTRIES) {
                return AnalysisOutcome.Failure(
                    AnalysisError.TooManyEntries(AnalysisLimits.MAX_ZIP_ENTRIES, rawEntries.size),
                )
            }

            val nameCounts = rawEntries.groupingBy { normalizeEntryName(it.name) }.eachCount()
            if ((nameCounts[ANDROID_MANIFEST_ENTRY_NAME] ?: 0) > 1) {
                return AnalysisOutcome.Failure(
                    AnalysisError.InvalidZip("duplicate $ANDROID_MANIFEST_ENTRY_NAME entries"),
                )
            }

            if (nameCounts[ANDROID_MANIFEST_ENTRY_NAME] == null) {
                return AnalysisOutcome.Failure(AnalysisError.MissingManifest())
            }

            val warnings = mutableListOf<String>()
            val invalidPathNames = mutableListOf<String>()
            var totalCompressed = 0L
            var totalUncompressed = 0L

            val entries =
                rawEntries.map { entry ->
                    val normalizedName = normalizeEntryName(entry.name)
                    if (!isSafeEntryName(entry.name)) {
                        invalidPathNames += entry.name
                    }

                    val compressed = entry.compressedSize.coerceAtLeast(0L)
                    val uncompressed =
                        if (entry.size < 0) {
                            warnings += "Unknown declared size for entry: $normalizedName"
                            0L
                        } else {
                            entry.size
                        }
                    totalCompressed += compressed
                    totalUncompressed += uncompressed

                    ArchiveEntryFact(
                        path = normalizedName,
                        isDirectory = entry.isDirectory,
                        compressedSize = compressed,
                        uncompressedSize = uncompressed,
                        category = categorize(normalizedName),
                        isDuplicate = (nameCounts[normalizedName] ?: 0) > 1,
                    )
                }

            if (invalidPathNames.isNotEmpty()) {
                warnings += "Entries with unsafe/traversal-like paths: ${invalidPathNames.take(5)}"
            }

            val duplicateCritical =
                nameCounts
                    .filterKeys { it in CRITICAL_SINGLETON_NAMES || isDexEntryName(it) }
                    .filterValues { it > 1 }
                    .keys
                    .toList()

            val expansionRatio = if (totalCompressed == 0L) 0L else (totalUncompressed + totalCompressed - 1) / totalCompressed
            val expansionExceedsPolicy = expansionRatio > AnalysisLimits.MAX_ZIP_EXPANSION_RATIO

            AnalysisOutcome.Success(
                ArchiveInventoryFact(
                    entries = entries,
                    totalCompressedSize = totalCompressed,
                    totalUncompressedSize = totalUncompressed,
                    duplicateCriticalEntryNames = duplicateCritical,
                    expansionRatioExceedsPolicy = expansionExceedsPolicy,
                    warnings = warnings,
                ),
            )
        }
    }
}

private fun isDexEntryName(name: String): Boolean = name.matches(Regex("classes\\d*\\.dex"))

/** Normalizes separators; never used to build a filesystem path, only for comparison/display. */
internal fun normalizeEntryName(name: String): String = name.replace('\\', '/')

internal fun isSafeEntryName(name: String): Boolean {
    if (name.isEmpty()) return false
    val normalized = normalizeEntryName(name)
    if (normalized.startsWith("/")) return false
    val segments = normalized.split("/")
    return segments.none { it == ".." }
}

internal fun categorize(path: String): ArchiveEntryCategory =
    when {
        path == ANDROID_MANIFEST_ENTRY_NAME -> ArchiveEntryCategory.COMPILED_MANIFEST
        path.matches(Regex("classes\\d*\\.dex")) -> ArchiveEntryCategory.DEX
        path.startsWith("lib/") -> ArchiveEntryCategory.NATIVE_LIBRARY
        path.startsWith("assets/") -> ArchiveEntryCategory.ASSETS
        path.startsWith("META-INF/") &&
            (path.endsWith(".RSA") || path.endsWith(".DSA") || path.endsWith(".EC") || path.endsWith(".SF") || path == "META-INF/MANIFEST.MF") ->
            ArchiveEntryCategory.SIGNING_METADATA
        path.startsWith("META-INF/") && (path.endsWith(".kotlin_module") || path.startsWith("META-INF/kotlin")) ->
            ArchiveEntryCategory.KOTLIN_METADATA
        path == "resources.arsc" || path.startsWith("res/") -> ArchiveEntryCategory.RESOURCES
        else -> ArchiveEntryCategory.OTHER
    }

/**
 * Reads a single bounded entry's decompressed bytes fully into memory. Callers must only use this
 * for entries expected to be small (e.g. `AndroidManifest.xml`); enforces [maxBytes] and aborts
 * rather than trusting the entry's declared size.
 */
fun readEntryBounded(zipFile: ZipFile, entry: ZipEntry, maxBytes: Int): AnalysisOutcome<ByteArray> {
    val stream =
        try {
            zipFile.getInputStream(entry)
        } catch (e: java.io.IOException) {
            return AnalysisOutcome.Failure(AnalysisError.UnreadableSource(e::class.simpleName))
        }

    return stream.use { input ->
        val buffer = java.io.ByteArrayOutputStream(minOf(maxBytes, 1 shl 20))
        val chunk = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val read = input.read(chunk)
            if (read == -1) break
            total += read
            if (total > maxBytes) {
                return AnalysisOutcome.Failure(AnalysisError.ResourceLimitExceeded("entry ${entry.name} exceeds $maxBytes bytes"))
            }
            buffer.write(chunk, 0, read)
        }
        AnalysisOutcome.Success(buffer.toByteArray())
    }
}
