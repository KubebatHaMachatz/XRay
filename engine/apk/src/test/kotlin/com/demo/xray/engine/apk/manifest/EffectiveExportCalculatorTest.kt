package com.demo.xray.engine.apk.manifest

import com.demo.xray.core.model.ComponentType
import com.demo.xray.core.model.ExportedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Comprehensive matrix test for [EffectiveExportCalculator].
 *
 * **Testing strategy**:
 *  - Uses parameterized testing (@RunWith(Parameterized)) to exhaustively cover the decision tree
 *  - 21 test cases covering all combinations of:
 *    * 5 component types (Activity, Activity-Alias, Service, Receiver, Provider)
 *    * 3 explicit export values (true, false, absent/null)
 *    * 2 intent-filter states (has filter, no filter)
 *    * 3 target SDK scenarios (pre-17, post-17, unknown)
 *  - Each case asserts the correct ExportedState is computed
 *  - Also validates that an explanation is generated (non-empty)
 *
 * **Why exhaustive testing matters here**:
 *  - The logic has multiple interacting dimensions (type, explicit, filters, SDK)
 *  - Missing a boundary case (e.g., the API-17 flip for providers) would silently create
 *    security vulnerabilities in the analysis
 *  - A malicious APK could exploit incorrect export-state calculation to evade detection
 *  - Thus, we test every significant path rather than just happy-path cases
 *
 * **Test data organization** (in companion object):
 *  - Grouped by rule (explicit wins, filter-based, provider legacy default)
 *  - Each line is a test case: [type, explicit, hasIntentFilter, targetSdk, expected]
 *  - Comments above each group explain the rule being tested
 */
@RunWith(Parameterized::class)
class EffectiveExportCalculatorMatrixTest(
    private val type: ComponentType,
    private val explicit: Boolean?,
    private val hasIntentFilter: Boolean,
    private val targetSdk: Int?,
    private val expected: ExportedState,
) {
    @Test
    fun `matches expected effective export state`() {
        // Invoke the calculator and verify it returns the expected state.
        val result = EffectiveExportCalculator.compute(type, explicit, hasIntentFilter, targetSdk)
        assertEquals(expected, result.state)

        // Also verify the explanation is present (for user-facing display).
        // An empty explanation would be unhelpful for auditing.
        assertTrue("explanation must not be blank", result.explanation.isNotBlank())
    }

    companion object {
        /**
         * Parameterized test data.
         *
         * JUnit runs one test for each array, using the first array element for the test name.
         * The test name format is: "index: type=... explicit=... hasFilter=... targetSdk=... -> expected"
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: type={0} explicit={1} hasFilter={2} targetSdk={3} -> {4}")
        fun data(): List<Array<Any?>> =
            listOf(
                // ============================================================================
                // RULE 1: Explicit android:exported attribute always wins
                // ============================================================================
                // If android:exported="true", component is exported regardless of type, filters, or SDK.
                arrayOf(ComponentType.ACTIVITY, true, false, 21, ExportedState.EXPORTED),
                // If android:exported="false", component is not exported, even with intent-filters.
                arrayOf(ComponentType.ACTIVITY, false, true, 33, ExportedState.NOT_EXPORTED),
                arrayOf(ComponentType.SERVICE, true, false, null, ExportedState.EXPORTED),
                arrayOf(ComponentType.RECEIVER, false, false, null, ExportedState.NOT_EXPORTED),
                arrayOf(ComponentType.PROVIDER, true, false, 10, ExportedState.EXPORTED),
                arrayOf(ComponentType.PROVIDER, false, false, 30, ExportedState.NOT_EXPORTED),
                arrayOf(ComponentType.ACTIVITY_ALIAS, true, false, null, ExportedState.EXPORTED),

                // ============================================================================
                // RULE 2: Activity/Alias/Service/Receiver implicit rule (android:exported absent)
                // ============================================================================
                // When explicit android:exported is absent, these components default based on
                // intent-filter presence. This does NOT depend on targetSdkVersion.
                arrayOf(ComponentType.ACTIVITY, null, true, 21, ExportedState.EXPORTED),
                arrayOf(ComponentType.ACTIVITY, null, false, 21, ExportedState.NOT_EXPORTED),
                arrayOf(ComponentType.ACTIVITY, null, true, 33, ExportedState.EXPORTED),
                arrayOf(ComponentType.ACTIVITY, null, false, null, ExportedState.NOT_EXPORTED),
                arrayOf(ComponentType.ACTIVITY_ALIAS, null, true, null, ExportedState.EXPORTED),
                arrayOf(ComponentType.SERVICE, null, true, 30, ExportedState.EXPORTED),
                arrayOf(ComponentType.SERVICE, null, false, 30, ExportedState.NOT_EXPORTED),
                arrayOf(ComponentType.RECEIVER, null, true, 16, ExportedState.EXPORTED),
                arrayOf(ComponentType.RECEIVER, null, false, 16, ExportedState.NOT_EXPORTED),

                // ============================================================================
                // RULE 3: Provider implicit rule (android:exported absent)
                // ============================================================================
                // Providers have a legacy default that flips at targetSdkVersion 17.
                // - targetSdk < 17: exported (backward compatibility for older apps)
                // - targetSdk >= 17: not exported (secure by default)
                // - targetSdk unknown: UNKNOWN (we can't confidently apply the rule)
                arrayOf(ComponentType.PROVIDER, null, false, 16, ExportedState.EXPORTED),
                arrayOf(ComponentType.PROVIDER, null, false, 17, ExportedState.NOT_EXPORTED),
                arrayOf(ComponentType.PROVIDER, null, false, 18, ExportedState.NOT_EXPORTED),
                arrayOf(ComponentType.PROVIDER, null, true, 16, ExportedState.EXPORTED),
                arrayOf(ComponentType.PROVIDER, null, false, null, ExportedState.UNKNOWN),
            )
    }
}
