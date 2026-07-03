package com.demo.xray.engine.rules.builtin

import com.demo.xray.core.model.AnalysisFacts
import com.demo.xray.core.model.FindingCategory
import com.demo.xray.core.model.Severity
import com.demo.xray.engine.rules.AnalysisRule
import com.demo.xray.engine.rules.RuleMetadata
import com.demo.xray.engine.rules.RuleResult

object AppDebuggableRule : AnalysisRule {
    override val id = "APP_DEBUGGABLE"
    override val metadata =
        RuleMetadata(
            title = "Application is debuggable",
            category = FindingCategory.MANIFEST_CONFIGURATION,
            defaultSeverity = Severity.HIGH,
            remediation = "Remove android:debuggable or ensure it is not set in release builds.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult =
        if (facts.manifest.applicationFlags.debuggable) {
            RuleResult.Finding(evidence = listOf("<application android:debuggable=\"true\">"))
        } else {
            RuleResult.Pass
        }
}

object AppTestOnlyRule : AnalysisRule {
    override val id = "APP_TEST_ONLY"
    override val metadata =
        RuleMetadata(
            title = "Application is marked test-only",
            category = FindingCategory.MANIFEST_CONFIGURATION,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Remove android:testOnly before distributing this build.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult =
        if (facts.manifest.applicationFlags.testOnly) {
            RuleResult.Finding(evidence = listOf("<application android:testOnly=\"true\">"))
        } else {
            RuleResult.Pass
        }
}

object CleartextTrafficAllowedRule : AnalysisRule {
    override val id = "CLEARTEXT_TRAFFIC_ALLOWED"
    override val metadata =
        RuleMetadata(
            title = "Cleartext network traffic is permitted",
            category = FindingCategory.MANIFEST_CONFIGURATION,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Set android:usesCleartextTraffic=\"false\" or configure a Network Security Config that restricts cleartext traffic.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val flags = facts.manifest.applicationFlags
        return when (flags.usesCleartextTraffic) {
            true -> RuleResult.Finding(evidence = listOf("<application android:usesCleartextTraffic=\"true\">"), confidence = com.demo.xray.core.model.Confidence.HIGH)
            false -> RuleResult.Pass
            null -> {
                val targetSdk = facts.manifest.usesSdk.targetSdkVersion
                if (targetSdk != null && targetSdk < 28) {
                    RuleResult.Finding(
                        evidence = listOf("android:usesCleartextTraffic is absent and targetSdkVersion ($targetSdk) is below 28, so cleartext traffic is allowed by platform default."),
                        confidence = com.demo.xray.core.model.Confidence.MEDIUM,
                        severityOverride = Severity.LOW,
                    )
                } else if (targetSdk == null) {
                    RuleResult.Unknown("targetSdkVersion is unknown; cannot determine the platform default for cleartext traffic.")
                } else {
                    RuleResult.Pass
                }
            }
        }
    }
}

object SharedUserIdDeclaredRule : AnalysisRule {
    override val id = "SHARED_USER_ID_DECLARED"
    override val metadata =
        RuleMetadata(
            title = "Deprecated sharedUserId is declared",
            category = FindingCategory.MANIFEST_CONFIGURATION,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Migrate away from android:sharedUserId; it is deprecated and Android no longer supports it for new installs on recent platform versions.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val sharedUserId = facts.manifest.sharedUserId
        return if (sharedUserId != null) {
            RuleResult.Finding(evidence = listOf("<manifest android:sharedUserId=\"$sharedUserId\">"))
        } else {
            RuleResult.Pass
        }
    }
}

object BackupConfigurationReviewRule : AnalysisRule {
    override val id = "BACKUP_CONFIGURATION_REVIEW"
    override val metadata =
        RuleMetadata(
            title = "Backup configuration should be reviewed",
            category = FindingCategory.MANIFEST_CONFIGURATION,
            defaultSeverity = Severity.LOW,
            remediation = "Review whether allowBackup and any backup/data-extraction rules are appropriate for the sensitivity of this app's data.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val allowBackup = facts.manifest.applicationFlags.allowBackup
        return if (allowBackup == false) {
            RuleResult.Pass
        } else {
            val stateText = if (allowBackup == null) "absent (defaults to true)" else "true"
            RuleResult.Finding(
                evidence = listOf("<application android:allowBackup> is $stateText"),
                confidence = com.demo.xray.core.model.Confidence.MEDIUM,
            )
        }
    }
}

object LegacyExternalStorageRule : AnalysisRule {
    override val id = "LEGACY_EXTERNAL_STORAGE"
    override val metadata =
        RuleMetadata(
            title = "Legacy external storage behavior requested",
            category = FindingCategory.MANIFEST_CONFIGURATION,
            defaultSeverity = Severity.LOW,
            remediation = "Migrate to scoped storage instead of android:requestLegacyExternalStorage.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult =
        if (facts.manifest.applicationFlags.requestLegacyExternalStorage == true) {
            RuleResult.Finding(evidence = listOf("<application android:requestLegacyExternalStorage=\"true\">"))
        } else {
            RuleResult.Pass
        }
}

object TargetSdkBelowPolicyRule : AnalysisRule {
    override val id = "TARGET_SDK_BELOW_POLICY"
    override val metadata =
        RuleMetadata(
            title = "Target SDK is below policy threshold",
            category = FindingCategory.MANIFEST_CONFIGURATION,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Raise targetSdkVersion to stay within current platform security and privacy defaults.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val targetSdk = facts.manifest.usesSdk.targetSdkVersion ?: return RuleResult.Unknown("targetSdkVersion is unknown.")
        return if (targetSdk < facts.targetSdkPolicyThreshold) {
            RuleResult.Finding(evidence = listOf("targetSdkVersion=$targetSdk is below the policy threshold of ${facts.targetSdkPolicyThreshold}."))
        } else {
            RuleResult.Pass
        }
    }
}
