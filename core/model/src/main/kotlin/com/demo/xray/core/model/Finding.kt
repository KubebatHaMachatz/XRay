package com.demo.xray.core.model

/**
 * Broad category of a finding, used to organize findings by area of concern.
 *
 * Each category groups related rules and helps users navigate large finding lists
 * by clicking through by-category views (future enhancement).
 */
enum class FindingCategory {
    SIGNING,                    // App signature / certificate validation
    MANIFEST_CONFIGURATION,     // AndroidManifest.xml flags and settings
    COMPONENT_EXPOSURE,         // Activities, services, receivers, providers
    PERMISSIONS,                // Requested permissions and risk assessment
    PACKAGING,                  // Archive structure, native code, resources
    NATIVE_CODE,                // ELF headers, ABI consistency, symbol stripping
    SDK,                        // Embedded third-party SDK detection and assessment
    COMPARISON,                 // Changes between two versions of the same app
}

/**
 * A single finding returned by the rules engine.
 *
 * A finding is a concrete, actionable piece of advice about the analyzed APK. It is produced
 * by a single rule (identified by [ruleId]) and contains:
 *  - A human-readable [title] and [explanation]
 *  - Risk assessment ([severity], [confidence])
 *  - [evidence]: a list of specific facts that triggered the rule
 *  - [affected]: optional component or permission name if the finding is about one item
 *  - [remediation]: developer-facing advice on how to fix it
 *
 * Design note: findings are immutable and data-class-based to support efficient filtering,
 * sorting, and caching (e.g., in a database or state management layer).
 */
data class Finding(
    /** Stable rule ID (e.g., "EXPORTED_PROVIDER_UNPROTECTED") for linking to documentation */
    val ruleId: String,

    /** Short user-visible title (e.g., "Exported content provider without permission check") */
    val title: String,

    /** Broad area of concern (see [FindingCategory]) */
    val category: FindingCategory,

    /** Risk level (INFO through CRITICAL) */
    val severity: Severity,

    /** Confidence in the detection */
    val confidence: Confidence,

    /** Paragraph-length explanation of the issue, why it matters, and who it affects */
    val explanation: String,

    /** List of specific evidence (e.g., ["Provider named: com.example.MyProvider", "No readPermission attribute"]) */
    val evidence: List<String>,

    /** Optional: the specific component, permission, or library that triggered the rule */
    val affected: String? = null,

    /** Actionable advice for the developer on how to fix or mitigate the issue */
    val remediation: String,

    /** Version of the rule set that produced this finding (for reproducibility and tracking) */
    val ruleSetVersion: String,
)
