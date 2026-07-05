package com.demo.xray.core.common

/**
 * Hard safety limits for untrusted APK input. See PRD FR-004. These are constants, not user
 * preferences, and every limit must be covered by a test that exercises the boundary.
 */
object AnalysisLimits {
    const val MAX_APK_FILE_SIZE_BYTES: Long = 1L * 1024 * 1024 * 1024 // 1 GiB

    const val MAX_ZIP_ENTRIES: Int = 100_000

    const val MAX_MANIFEST_ENTRY_BYTES: Int = 16 * 1024 * 1024 // 16 MiB

    const val MAX_SINGLE_TEXT_ENTRY_BYTES: Int = 32 * 1024 * 1024 // 32 MiB

    const val MAX_AGGREGATE_DECOMPRESSED_BYTES: Long = 2L * 1024 * 1024 * 1024 // 2 GiB

    const val MAX_ZIP_EXPANSION_RATIO: Long = 1_000L

    /** FR-073 / RULE LARGE_UNCOMPRESSED_ENTRY default threshold. */
    const val LARGE_UNCOMPRESSED_ENTRY_BYTES: Long = 50L * 1024 * 1024 // 50 MiB

    /** RULE TARGET_SDK_BELOW_POLICY default threshold; adjust as Android releases advance. */
    const val DEFAULT_TARGET_SDK_POLICY_THRESHOLD: Int = 33

    /** FR-134 comparison size-regression thresholds. */
    const val SIGNIFICANT_SIZE_INCREASE_ABSOLUTE_BYTES: Long = 5L * 1024 * 1024 // 5 MiB
    const val SIGNIFICANT_SIZE_INCREASE_RELATIVE_PERCENT: Int = 10
}
