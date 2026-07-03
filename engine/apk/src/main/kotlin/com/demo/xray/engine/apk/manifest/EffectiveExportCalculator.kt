package com.demo.xray.engine.apk.manifest

import com.demo.xray.core.model.ComponentType
import com.demo.xray.core.model.ExportedState

/**
 * Pure, deterministic computation of a component's effective exported state per PRD FR-033.
 *
 * **Why this matters**: The `android:exported` attribute controls whether a component is
 * accessible to other apps (or just private to this app). When it's absent from the manifest,
 * Android applies implicit rules based on component type and target SDK level. Incorrectly
 * assessing exported state is a major source of security vulnerabilities (exposed components,
 * BROADCAST_RECEIVER injection, etc.).
 *
 * **Design**: This is a pure, stateless function (no side effects, framework dependencies, or I/O)
 * so it can be thoroughly unit-tested on the JVM without requiring a device or emulator.
 * The function is table-driven (not nested if-else) for clarity and to avoid logic errors.
 *
 * **Decision tree** (applied in order):
 *
 * 1. **Explicit `android:exported`**: If present in the manifest, its value wins, period.
 *    No component type or SDK-level considerations override an explicit attribute.
 *
 * 2. **Provider (implicit rules)**:
 *    - If `targetSdkVersion < 17`: defaults to EXPORTED (legacy `ContentProvider` default)
 *    - If `targetSdkVersion >= 17`: defaults to NOT_EXPORTED
 *    - If `targetSdkVersion` unknown: return UNKNOWN (we can't confidently apply the rule)
 *
 * 3. **Activity / Activity-alias / Service / Receiver (implicit rules)**:
 *    - If it has at least one `<intent-filter>`: defaults to EXPORTED
 *      (making it implicitly launchable by other apps)
 *    - If no intent-filter: defaults to NOT_EXPORTED
 *    - Note: intent-filter-based export does NOT depend on target SDK; the rule is stable
 *      across API levels (Android requires explicit android:exported for API 31+, but when
 *      it's absent, the platform still falls back to this rule)
 *
 * Test coverage: 21 parameterized cases covering all component types, explicit true/false/absent,
 * intent-filter presence, and SDK boundaries (see EffectiveExportCalculatorMatrixTest.kt).
 */
object EffectiveExportCalculator {
    /** Pair of computed state and human-readable explanation for why it was computed that way. */
    data class Result(val state: ExportedState, val explanation: String)

    /** API level where ContentProvider default behavior changed from exported to not-exported. */
    private const val PROVIDER_LEGACY_DEFAULT_MAX_TARGET_SDK = 17

    /**
     * Compute the effective exported state for a component.
     *
     * @param type Component type (Activity, Service, Receiver, Provider, or ActivityAlias)
     * @param explicitExported Raw android:exported attribute if present (null if absent)
     * @param hasIntentFilter True if the component declares at least one <intent-filter>
     * @param targetSdkVersion Target API level (null if not specified in manifest)
     * @return Result containing computed state and explanation
     */
    fun compute(
        type: ComponentType,
        explicitExported: Boolean?,
        hasIntentFilter: Boolean,
        targetSdkVersion: Int?,
    ): Result {
        // Explicit attribute always wins.
        if (explicitExported != null) {
            val state = if (explicitExported) ExportedState.EXPORTED else ExportedState.NOT_EXPORTED
            return Result(state, "Explicit android:exported=\"$explicitExported\" declared in the manifest.")
        }

        // Apply implicit rules based on component type.
        return if (type == ComponentType.PROVIDER) {
            computeProviderDefault(targetSdkVersion)
        } else {
            computeIntentFilterDefault(type, hasIntentFilter)
        }
    }

    /**
     * Implicit export rules for ContentProvider (type == PROVIDER).
     *
     * The most complex case: providers have a legacy default that depends on targetSdkVersion.
     * This reflects the API 17 (JELLY_BEAN_MR1) behavior change. Apps targeting older APIs
     * get backward-compatible behavior (exported by default); newer apps must be explicit.
     */
    private fun computeProviderDefault(targetSdkVersion: Int?): Result =
        when {
            targetSdkVersion == null ->
                // Can't apply the rule without knowing targetSdkVersion.
                Result(
                    ExportedState.UNKNOWN,
                    "android:exported is absent and targetSdkVersion is unknown, so the provider's " +
                        "legacy default (exported only if targetSdkVersion < $PROVIDER_LEGACY_DEFAULT_MAX_TARGET_SDK) cannot be determined.",
                )
            targetSdkVersion < PROVIDER_LEGACY_DEFAULT_MAX_TARGET_SDK ->
                // Targeting pre-API-17: provider is exported for backward compatibility.
                Result(
                    ExportedState.EXPORTED,
                    "android:exported is absent; providers default to exported when targetSdkVersion " +
                        "($targetSdkVersion) is below $PROVIDER_LEGACY_DEFAULT_MAX_TARGET_SDK.",
                )
            else ->
                // Targeting API-17 or later: provider is not exported by default.
                Result(
                    ExportedState.NOT_EXPORTED,
                    "android:exported is absent; providers default to not exported when targetSdkVersion " +
                        "($targetSdkVersion) is $PROVIDER_LEGACY_DEFAULT_MAX_TARGET_SDK or higher.",
                )
        }

    /**
     * Implicit export rules for Activity, Service, Receiver (and ActivityAlias).
     *
     * Simple rule: if the component has an intent-filter, it's implicitly exported
     * (other apps can send matching intents to it); otherwise it's private.
     * This does NOT depend on targetSdkVersion.
     */
    private fun computeIntentFilterDefault(type: ComponentType, hasIntentFilter: Boolean): Result {
        val typeLabel = type.name.lowercase().replace('_', ' ')
        return if (hasIntentFilter) {
            Result(
                ExportedState.EXPORTED,
                "android:exported is absent; this $typeLabel declares at least one <intent-filter>, so it defaults to exported.",
            )
        } else {
            Result(
                ExportedState.NOT_EXPORTED,
                "android:exported is absent and this $typeLabel has no <intent-filter>, so it defaults to not exported.",
            )
        }
    }
}
