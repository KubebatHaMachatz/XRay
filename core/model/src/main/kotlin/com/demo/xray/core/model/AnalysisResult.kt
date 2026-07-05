package com.demo.xray.core.model

data class ImportedApkFact(
    val sha256: String,
    val fileSizeBytes: Long,
    val sourceDisplayName: String?,
)

/**
 * Result of cross-checking manifest-parser output against Android's public
 * `PackageManager.getPackageArchiveInfo()` API (PRD FR-022). Populated by the `:app` layer,
 * which is the only layer allowed to depend on Android framework types.
 */
data class PackageManagerCrossCheckFact(
    val applicationLabel: String?,
    val iconLoaded: Boolean,
    val discrepancies: List<String> = emptyList(),
)

/** Aggregate input to the rule engine. Pure Kotlin, no Android framework types. */
data class AnalysisFacts(
    val imported: ImportedApkFact,
    val manifest: ManifestFacts,
    val archive: ArchiveInventoryFact,
    val targetSdkPolicyThreshold: Int,
    val packageManagerCrossCheck: PackageManagerCrossCheckFact? = null,
)

data class AnalysisResult(
    val jobId: String,
    val facts: AnalysisFacts,
    val findings: List<Finding>,
    val engineVersion: String,
    val ruleSetVersion: String,
    val analyzedAtEpochMs: Long,
    val durationMs: Long,
    val status: AnalysisJobState,
)
