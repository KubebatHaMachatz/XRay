package com.demo.xray.core.model

/**
 * Severity of a rule finding. Ordered from least to most severe.
 *
 * This is the primary axis for risk assessment in the UI and for filtering. Each finding
 * is assigned exactly one severity based on the rule definition and evidence.
 *
 * See also: [Confidence], which measures how certain the engine is (independent of severity).
 */
enum class Severity {
    INFO,    // Informational; no security concern
    LOW,     // Minor security or best-practice issue
    MEDIUM,  // Moderate risk; recommend addressing
    HIGH,    // Significant security risk; strongly recommend fixing
    CRITICAL, // Critical security flaw; immediate action required
}

/**
 * How confident the engine is in a finding or detection, independent of severity.
 *
 * A finding can be HIGH severity but LOW confidence (we think this is bad, but we're not sure),
 * or LOW severity but HIGH confidence (we're certain this isn't a best practice).
 */
enum class Confidence {
    LOW,    // Heuristic or circumstantial evidence
    MEDIUM, // Evidence is clear but may have false positives in edge cases
    HIGH,   // Direct/explicit evidence; high fidelity
}

/**
 * Where a piece of displayed data was derived from (for future UI attribution in FR-021).
 *
 * This enum tracks the source of facts and findings so the UI can display confidence/provenance
 * information. Currently modeled but not yet displayed; see IMPLEMENTATION_STATUS.md.
 */
enum class FieldSource {
    MANIFEST,             // Parsed from AndroidManifest.xml
    PACKAGE_MANAGER_ARCHIVE, // From installed PackageInfo on device
    ZIP_INVENTORY,        // From ZIP archive structure (no content parsing)
    DEX_SCAN,             // From DEX bytecode analysis (future)
    HEURISTIC,            // Derived via heuristic or inference
}

/**
 * Android component types that can be defined in the manifest.
 * Each has different export rules and security implications.
 */
enum class ComponentType {
    ACTIVITY,       // User-facing UI screen
    ACTIVITY_ALIAS, // Alias for another activity
    SERVICE,        // Background work without UI
    RECEIVER,       // Broadcast receiver for system/app events
    PROVIDER,       // Content provider; data sharing mechanism
}

/**
 * Risk category for a permission, based on the bundled permission knowledge base.
 *
 * Permissions are grouped by how much risk they pose to user privacy/security.
 * This comes from Android's own permission tiering plus domain knowledge.
 */
enum class PermissionRiskCategory {
    NORMAL,           // Pre-granted; no user consent needed
    SENSITIVE,        // User must grant explicitly; involves personal data
    PRIVILEGED_SYSTEM, // System-only or pre-installed apps only
    SPECIAL_ACCESS,   // Requires settings toggle (e.g., WRITE_SETTINGS, PACKAGE_USAGE_STATS)
    UNKNOWN,          // Not in the bundled knowledge base; be conservative
}

/**
 * Effective exported state of a component, computed per PRD FR-033.
 *
 * This is distinct from the raw `android:exported` manifest attribute, which may be absent.
 * The effective state accounts for:
 *  - Explicit android:exported value (if present, always wins)
 *  - Component type and presence of intent-filters
 *  - Target SDK and API-level defaults (e.g., Provider defaults to exported on API < 17)
 *
 * See EffectiveExportCalculator for the full decision table.
 */
enum class ExportedState {
    EXPORTED,     // Component is accessible to other apps or the system
    NOT_EXPORTED, // Component is private to this app
    UNKNOWN,      // Cannot determine (e.g., targetSdk not known)
}
