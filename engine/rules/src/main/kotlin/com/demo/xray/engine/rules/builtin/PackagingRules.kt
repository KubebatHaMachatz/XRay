package com.demo.xray.engine.rules.builtin

import com.demo.xray.core.common.AnalysisLimits
import com.demo.xray.core.model.AnalysisFacts
import com.demo.xray.core.model.FindingCategory
import com.demo.xray.core.model.Severity
import com.demo.xray.engine.rules.AnalysisRule
import com.demo.xray.engine.rules.RuleMetadata
import com.demo.xray.engine.rules.RuleResult

private val ABI_64_BIT = setOf("arm64-v8a", "x86_64")
private val ABI_32_BIT = setOf("armeabi", "armeabi-v7a", "x86")

private fun nativeLibraryAbiDirectories(facts: AnalysisFacts): Map<String, Set<String>> {
    val libs = facts.archive.entries.filter { !it.isDirectory && it.path.startsWith("lib/") }
    // path like lib/<abi>/libfoo.so
    return libs
        .mapNotNull { entry ->
            val parts = entry.path.split("/")
            if (parts.size >= 3) parts[1] to parts.drop(2).joinToString("/") else null
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.toSet() }
}

object Only32BitNativeLibsRule : AnalysisRule {
    override val id = "ONLY_32_BIT_NATIVE_LIBS"
    override val metadata =
        RuleMetadata(
            title = "Only 32-bit native libraries present",
            category = FindingCategory.NATIVE_CODE,
            defaultSeverity = Severity.LOW,
            remediation = "Add 64-bit (arm64-v8a / x86_64) native libraries; Google Play requires 64-bit support for apps with native code.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val abiDirs = nativeLibraryAbiDirectories(facts).keys
        if (abiDirs.isEmpty()) return RuleResult.NotApplicable
        val has32 = abiDirs.any { it in ABI_32_BIT }
        val has64 = abiDirs.any { it in ABI_64_BIT }
        return if (has32 && !has64) {
            RuleResult.Finding(evidence = listOf("Native ABI directories present: ${abiDirs.sorted()}"))
        } else {
            RuleResult.Pass
        }
    }
}

object NativeLibraryAbiMismatchRule : AnalysisRule {
    override val id = "NATIVE_LIBRARY_ABI_MISMATCH"
    override val metadata =
        RuleMetadata(
            title = "Native libraries are inconsistent across ABIs",
            category = FindingCategory.NATIVE_CODE,
            defaultSeverity = Severity.LOW,
            remediation = "Ensure every native library present for one ABI is also built for the app's other supported ABIs, or the app may crash on some devices.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val byAbi = nativeLibraryAbiDirectories(facts)
        if (byAbi.size < 2) return RuleResult.NotApplicable
        val allLibs = byAbi.values.flatten().toSet()
        val mismatches =
            byAbi.entries.flatMap { (abi, libs) ->
                (allLibs - libs).map { missing -> "$abi is missing $missing" }
            }
        return if (mismatches.isEmpty()) {
            RuleResult.Pass
        } else {
            RuleResult.Finding(evidence = mismatches)
        }
    }
}

object LargeUncompressedEntryRule : AnalysisRule {
    override val id = "LARGE_UNCOMPRESSED_ENTRY"
    override val metadata =
        RuleMetadata(
            title = "Archive contains a very large entry",
            category = FindingCategory.PACKAGING,
            defaultSeverity = Severity.LOW,
            remediation = "Review whether this entry needs to ship uncompressed at this size, e.g. by compressing assets or downloading them on demand.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val offenders = facts.archive.entries.filter { it.uncompressedSize > AnalysisLimits.LARGE_UNCOMPRESSED_ENTRY_BYTES }
        return if (offenders.isEmpty()) {
            RuleResult.Pass
        } else {
            RuleResult.Finding(
                evidence = offenders.map { "${it.path}: ${it.uncompressedSize} bytes uncompressed" },
                affected = offenders.joinToString(", ") { it.path },
            )
        }
    }
}

object ExtremeZipExpansionRatioRule : AnalysisRule {
    override val id = "EXTREME_ZIP_EXPANSION_RATIO"
    override val metadata =
        RuleMetadata(
            title = "Archive has an extreme compression expansion ratio",
            category = FindingCategory.PACKAGING,
            defaultSeverity = Severity.HIGH,
            remediation = "An extreme expansion ratio can indicate a decompression-bomb-style packaging pattern; treat with caution before further processing.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult =
        if (facts.archive.expansionRatioExceedsPolicy) {
            RuleResult.Finding(
                evidence =
                    listOf(
                        "Total compressed=${facts.archive.totalCompressedSize} bytes, " +
                            "uncompressed=${facts.archive.totalUncompressedSize} bytes",
                    ),
            )
        } else {
            RuleResult.Pass
        }
}

object DuplicateCriticalEntryRule : AnalysisRule {
    override val id = "DUPLICATE_CRITICAL_ENTRY"
    override val metadata =
        RuleMetadata(
            title = "Duplicate critical archive entry",
            category = FindingCategory.PACKAGING,
            defaultSeverity = Severity.HIGH,
            remediation = "A duplicate critical entry name (manifest or DEX file) creates ambiguous interpretation across tools and Android versions; rebuild the APK to remove it.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val names = facts.archive.duplicateCriticalEntryNames
        return if (names.isEmpty()) {
            RuleResult.Pass
        } else {
            RuleResult.Finding(evidence = names, affected = names.joinToString(", "))
        }
    }
}
