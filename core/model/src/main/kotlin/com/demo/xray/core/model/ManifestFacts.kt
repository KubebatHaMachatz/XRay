package com.demo.xray.core.model

/**
 * Parsed `<uses-sdk>` element from the manifest.
 *
 * These values are critical for security analysis:
 *  - [minSdkVersion] is the earliest API level the app can run on
 *  - [targetSdkVersion] affects permission defaults and behavior changes (e.g., export state)
 *  - [maxSdkVersion] (rarely used) is the latest API level allowed
 */
data class UsesSdkFact(
    val minSdkVersion: Int?,
    val targetSdkVersion: Int?,
    val maxSdkVersion: Int?,
)

/**
 * A permission requested via `<uses-permission>`.
 *
 * Includes cross-referenced data from the bundled permission knowledge base (offline, no network).
 * The [riskCategory] and [friendlyName] are only non-null if the permission is in the database;
 * custom/unknown permissions get [riskCategory] = UNKNOWN.
 */
data class PermissionFact(
    /** Permission name (e.g., "android.permission.CAMERA") */
    val name: String,

    /** True if this is a custom permission (defined by this or another app), not a system permission */
    val isCustom: Boolean,

    /** Protection level declared in manifest (e.g., "dangerous", "signature"); may differ from system's level */
    val protectionLevel: String?,

    /** If maxSdkVersion is set, permission is only requested up to this API level */
    val maxSdkVersion: Int?,

    /** Manifest attribute: feature that must be present on the device for this permission to apply */
    val requiredFeature: String?,

    /** Manifest attribute: feature that must NOT be present for this permission to apply */
    val requiredNotFeature: String?,

    /** Manifest attribute flags (bit-packed; not commonly used) */
    val usesPermissionFlags: Int?,

    /** Risk category from knowledge base (NORMAL, SENSITIVE, PRIVILEGED_SYSTEM, SPECIAL_ACCESS, UNKNOWN) */
    val riskCategory: PermissionRiskCategory,

    /** Human-readable permission name from knowledge base (e.g., "Camera") */
    val friendlyName: String?,

    /** Permission group (e.g., "android.permission-group.CAMERA") */
    val group: String?,

    /** One-sentence explanation of what this permission is for and why it's risky */
    val explanation: String?,
)

/**
 * A permission defined by this app via `<permission>`.
 *
 * Custom permissions are often used to gate access to this app's components or content provider.
 * If they are weak (e.g., protectionLevel="normal"), other apps can easily satisfy them.
 */
data class CustomPermissionFact(
    val name: String,
    val protectionLevel: String?,
    val label: String?,
    val description: String?,

    /** Components (activities, services, etc.) that declare this permission in their android:permission attribute */
    val guardedComponents: List<String> = emptyList(),

    /** True if this permission is weak (e.g., "normal" protection) for guarding an exported component */
    val isWeakForExportedUse: Boolean = false,
)

/**
 * A `<data>` element within an `<intent-filter>`.
 *
 * Intent-filter data elements specify which implicit intents a component can handle
 * (by scheme, host, MIME type, etc.).
 */
data class IntentFilterDataFact(
    val scheme: String? = null,      // e.g., "http", "https", "content"
    val host: String? = null,        // e.g., "example.com"
    val port: String? = null,        // e.g., "8080"
    val path: String? = null,        // Exact path match
    val pathPrefix: String? = null,  // Prefix match
    val pathPattern: String? = null, // Regex pattern match
    val mimeType: String? = null,    // e.g., "image/*", "text/plain"
)

/**
 * An `<intent-filter>` element from a component.
 *
 * Intent-filters make components implicitly exported (can be launched by other apps)
 * even if android:exported is not explicitly set, depending on target SDK.
 */
data class IntentFilterFact(
    /** Implicit-intent actions this component handles (e.g., "android.intent.action.VIEW") */
    val actions: List<String> = emptyList(),

    /** Intent categories (e.g., "android.intent.category.LAUNCHER") */
    val categories: List<String> = emptyList(),

    /** Data elements defining what URIs/MIME types this component accepts */
    val data: List<IntentFilterDataFact> = emptyList(),

    /** Priority for intent resolution (higher = more likely to be chosen by system) */
    val priority: Int? = null,

    /** autoVerify attribute (for web intent filters; requires domain verification) */
    val autoVerify: Boolean = false,
)

/**
 * A component (Activity, Service, Receiver, or Provider) from the manifest.
 *
 * This is the core of security analysis. Whether a component is exported, what permissions
 * protect it, and what intent-filters it declares determine its attack surface.
 *
 * Design note: Component-specific fields (e.g., Provider-only: [authorities], [readPermission])
 * are included in a single data class for simplicity; unused fields are empty/null for other types.
 */
data class ComponentFact(
    /** Component class name (usually fully qualified, e.g., "com.example.MainActivity") */
    val name: String,

    /** Component type (Activity, Service, Receiver, Provider, or ActivityAlias) */
    val type: ComponentType,

    /** android:enabled attribute; false means it's disabled and won't be invoked */
    val enabled: Boolean,

    /** Raw android:exported attribute value if explicitly set (null if absent from manifest) */
    val explicitExported: Boolean?,

    /** Computed effective export state per PRD FR-033 (accounts for implicit export rules) */
    val effectiveExported: ExportedState,

    /** Human-readable explanation of why effectiveExported was computed that way */
    val exportedExplanation: String,

    /** Permission that other apps must have to interact with this component */
    val permission: String?,

    /** Process name where this component runs (empty = runs in app's default process) */
    val processName: String?,

    /** Whether this component runs on the lock screen (API 24+) */
    val directBootAware: Boolean,

    val intentFilters: List<IntentFilterFact> = emptyList(),
    val metaData: Map<String, String> = emptyMap(),

    // ========== Provider-only fields ==========
    /** Content provider authorities (e.g., "com.example.MyProvider") */
    val authorities: List<String> = emptyList(),

    /** Permission required to read from this provider */
    val readPermission: String? = null,

    /** Permission required to write to this provider */
    val writePermission: String? = null,

    /** Whether this provider can grant URI permissions via grantUriPermission */
    val grantUriPermissions: Boolean = false,

    /** multiprocess attribute (whether other apps can have instances in their own process) */
    val multiprocess: Boolean? = null,

    /** Provider initialization order */
    val initOrder: Int? = null,

    // ========== Activity/ActivityAlias-only fields ==========
    /** True if this activity appears in the launcher (has LAUNCHER intent-filter) */
    val isLauncher: Boolean = false,

    /** For activity-alias: the target activity this alias points to */
    val targetActivity: String? = null,
)

/**
 * Application-wide flags from the `<application>` manifest element.
 *
 * These settings affect security, debugging, and backup behavior across the entire app.
 */
data class ApplicationFlags(
    /** App display name (human-readable label) */
    val label: String?,

    /** If true, the app is debuggable (allows arbitrary code injection via debugger) */
    val debuggable: Boolean,

    /** If true, the app is marked as a test-only app (rejected by Play Store for production) */
    val testOnly: Boolean,

    /** Whether backup is enabled (can leak sensitive data via adb backup) */
    val allowBackup: Boolean?,

    /** Whether cleartext (unencrypted) HTTP traffic is allowed (insecure) */
    val usesCleartextTraffic: Boolean?,

    /** Whether native libraries are extracted to disk (vs. loaded directly from APK) */
    val extractNativeLibs: Boolean?,

    /** requestLegacyExternalStorage compatibility flag (for targeting API < 30 on modern systems) */
    val requestLegacyExternalStorage: Boolean?,

    /** If true, a network-security-config.xml file exists (defines certificate pinning, etc.) */
    val hasNetworkSecurityConfig: Boolean,

    /** Whether to request a large heap (android:largeHeap) */
    val largeHeap: Boolean,

    /** If false, the app has no code (rare; usually it's assets-only) */
    val hasCode: Boolean,

    /** Manifest-level android:exported attribute (rarely set at app level; null if not present) */
    val manifestExported: Boolean? = null,
)

/**
 * An `<instrumentation>` element (usually for test instrumentation).
 *
 * Instrumentation allows one app to inject code into another app's process for testing.
 */
data class InstrumentationFact(
    val name: String,

    /** Target package that this instrumentation instruments */
    val targetPackage: String?,

    val functionalTest: Boolean?,
    val handleProfiling: Boolean?,
)

/**
 * Complete parsed manifest facts, assembled by ManifestAnalyzer.
 *
 * This is the primary input to the rules engine. It contains all security-relevant facts
 * extracted from the binary AndroidManifest.xml (via the AXML parser), plus any warnings
 * encountered during parsing (e.g., malformed intent-filters).
 *
 * Design: this is a pure domain model with no Android framework dependencies, making it
 * testable on the JVM without needing an emulator or device.
 */
data class ManifestFacts(
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val compileSdkVersion: Int?,

    val usesSdk: UsesSdkFact,
    val sharedUserId: String?,

    /** All permissions requested via `<uses-permission>` */
    val permissions: List<PermissionFact> = emptyList(),

    /** All permissions defined by this app via `<permission>` */
    val customPermissions: List<CustomPermissionFact> = emptyList(),

    /** All features required or optional via `<uses-feature>` */
    val usesFeatures: List<String> = emptyList(),

    // ========== Package visibility filtering (Android 11+) ==========
    /** Specific packages this app declares it can interact with */
    val queriesPackages: List<String> = emptyList(),

    /** Specific implicit-intent actions this app might send */
    val queriesIntentActions: List<String> = emptyList(),

    /** Specific content-provider authorities this app might query */
    val queriesProviderAuthorities: List<String> = emptyList(),

    /** If true, the app requests QUERY_ALL_PACKAGES (can see all installed packages) */
    val queriesAllPackages: Boolean = false,

    val applicationFlags: ApplicationFlags,

    /** All components (activities, services, receivers, providers) */
    val components: List<ComponentFact> = emptyList(),

    /** Libraries required by this app (e.g., androidx.window.extensions) */
    val usesLibraries: List<String> = emptyList(),

    /** Instrumentation declarations */
    val instrumentation: List<InstrumentationFact> = emptyList(),

    /** Any non-fatal warnings encountered during manifest parsing or analysis */
    val warnings: List<String> = emptyList(),
)
