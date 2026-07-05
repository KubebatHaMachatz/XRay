package com.demo.xray.engine.rules.builtin

import com.demo.xray.engine.rules.RuleResult
import com.demo.xray.engine.rules.baseApplicationFlags
import com.demo.xray.engine.rules.baseFacts
import com.demo.xray.engine.rules.baseManifest
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestFlagRulesTest {
    @Test
    fun `APP_DEBUGGABLE triggers when debuggable is true`() {
        val facts = baseFacts(manifest = baseManifest(applicationFlags = baseApplicationFlags().copy(debuggable = true)))
        assertTrue(AppDebuggableRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `APP_DEBUGGABLE passes when debuggable is false`() {
        val facts = baseFacts()
        assertTrue(AppDebuggableRule.evaluate(facts) is RuleResult.Pass)
    }

    @Test
    fun `APP_TEST_ONLY triggers when testOnly is true`() {
        val facts = baseFacts(manifest = baseManifest(applicationFlags = baseApplicationFlags().copy(testOnly = true)))
        assertTrue(AppTestOnlyRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `CLEARTEXT_TRAFFIC_ALLOWED triggers on explicit true`() {
        val facts = baseFacts(manifest = baseManifest(applicationFlags = baseApplicationFlags().copy(usesCleartextTraffic = true)))
        assertTrue(CleartextTrafficAllowedRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `CLEARTEXT_TRAFFIC_ALLOWED passes on explicit false`() {
        val facts = baseFacts()
        assertTrue(CleartextTrafficAllowedRule.evaluate(facts) is RuleResult.Pass)
    }

    @Test
    fun `CLEARTEXT_TRAFFIC_ALLOWED triggers when absent and targetSdk below 28`() {
        val facts =
            baseFacts(
                manifest =
                    baseManifest(
                        applicationFlags = baseApplicationFlags().copy(usesCleartextTraffic = null),
                        targetSdkVersion = 26,
                    ),
            )
        assertTrue(CleartextTrafficAllowedRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `CLEARTEXT_TRAFFIC_ALLOWED passes when absent and targetSdk 28 or above`() {
        val facts =
            baseFacts(
                manifest =
                    baseManifest(
                        applicationFlags = baseApplicationFlags().copy(usesCleartextTraffic = null),
                        targetSdkVersion = 30,
                    ),
            )
        assertTrue(CleartextTrafficAllowedRule.evaluate(facts) is RuleResult.Pass)
    }

    @Test
    fun `CLEARTEXT_TRAFFIC_ALLOWED is unknown when absent and targetSdk unknown`() {
        val facts =
            baseFacts(
                manifest =
                    baseManifest(
                        applicationFlags = baseApplicationFlags().copy(usesCleartextTraffic = null),
                        targetSdkVersion = null,
                    ),
            )
        assertTrue(CleartextTrafficAllowedRule.evaluate(facts) is RuleResult.Unknown)
    }

    @Test
    fun `SHARED_USER_ID_DECLARED triggers when present`() {
        val facts = baseFacts(manifest = baseManifest(sharedUserId = "com.example.shared"))
        assertTrue(SharedUserIdDeclaredRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `SHARED_USER_ID_DECLARED passes when absent`() {
        assertTrue(SharedUserIdDeclaredRule.evaluate(baseFacts()) is RuleResult.Pass)
    }

    @Test
    fun `BACKUP_CONFIGURATION_REVIEW passes only when explicitly disabled`() {
        assertTrue(BackupConfigurationReviewRule.evaluate(baseFacts()) is RuleResult.Pass)
        val allowed = baseFacts(manifest = baseManifest(applicationFlags = baseApplicationFlags().copy(allowBackup = true)))
        assertTrue(BackupConfigurationReviewRule.evaluate(allowed) is RuleResult.Finding)
        val absent = baseFacts(manifest = baseManifest(applicationFlags = baseApplicationFlags().copy(allowBackup = null)))
        assertTrue(BackupConfigurationReviewRule.evaluate(absent) is RuleResult.Finding)
    }

    @Test
    fun `LEGACY_EXTERNAL_STORAGE triggers when requested`() {
        val facts = baseFacts(manifest = baseManifest(applicationFlags = baseApplicationFlags().copy(requestLegacyExternalStorage = true)))
        assertTrue(LegacyExternalStorageRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `TARGET_SDK_BELOW_POLICY triggers below threshold and passes above`() {
        val below = baseFacts(manifest = baseManifest(targetSdkVersion = 28), targetSdkPolicyThreshold = 33)
        assertTrue(TargetSdkBelowPolicyRule.evaluate(below) is RuleResult.Finding)

        val above = baseFacts(manifest = baseManifest(targetSdkVersion = 34), targetSdkPolicyThreshold = 33)
        assertTrue(TargetSdkBelowPolicyRule.evaluate(above) is RuleResult.Pass)
    }

    @Test
    fun `TARGET_SDK_BELOW_POLICY is unknown when targetSdk missing`() {
        val facts = baseFacts(manifest = baseManifest(targetSdkVersion = null))
        assertTrue(TargetSdkBelowPolicyRule.evaluate(facts) is RuleResult.Unknown)
    }
}
