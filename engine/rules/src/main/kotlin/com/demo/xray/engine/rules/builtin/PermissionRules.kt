package com.demo.xray.engine.rules.builtin

import com.demo.xray.core.model.AnalysisFacts
import com.demo.xray.core.model.FindingCategory
import com.demo.xray.core.model.Severity
import com.demo.xray.engine.rules.AnalysisRule
import com.demo.xray.engine.rules.RuleMetadata
import com.demo.xray.engine.rules.RuleResult

private fun AnalysisFacts.hasPermission(name: String): Boolean = manifest.permissions.any { it.name == name }

object QueryAllPackagesRequestedRule : AnalysisRule {
    override val id = "QUERY_ALL_PACKAGES_REQUESTED"
    override val metadata =
        RuleMetadata(
            title = "Broad package visibility requested",
            category = FindingCategory.PERMISSIONS,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Prefer <queries> declarations scoped to the specific packages/intents this app needs instead of QUERY_ALL_PACKAGES.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult =
        if (facts.manifest.queriesAllPackages) {
            RuleResult.Finding(evidence = listOf("<uses-permission android:name=\"android.permission.QUERY_ALL_PACKAGES\">"))
        } else {
            RuleResult.Pass
        }
}

object RequestInstallPackagesRule : AnalysisRule {
    override val id = "REQUEST_INSTALL_PACKAGES"
    override val metadata =
        RuleMetadata(
            title = "App can request installation of other packages",
            category = FindingCategory.PERMISSIONS,
            defaultSeverity = Severity.HIGH,
            remediation = "Confirm this capability is required; it allows the app to trigger installation of other APKs.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult =
        if (facts.hasPermission("android.permission.REQUEST_INSTALL_PACKAGES")) {
            RuleResult.Finding(evidence = listOf("<uses-permission android:name=\"android.permission.REQUEST_INSTALL_PACKAGES\">"))
        } else {
            RuleResult.Pass
        }
}

object SystemAlertWindowRequestedRule : AnalysisRule {
    override val id = "SYSTEM_ALERT_WINDOW_REQUESTED"
    override val metadata =
        RuleMetadata(
            title = "App can draw over other apps",
            category = FindingCategory.PERMISSIONS,
            defaultSeverity = Severity.HIGH,
            remediation = "Confirm this overlay capability is required; it can be used for UI redressing/clickjacking-style attacks if misused.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult =
        if (facts.hasPermission("android.permission.SYSTEM_ALERT_WINDOW")) {
            RuleResult.Finding(evidence = listOf("<uses-permission android:name=\"android.permission.SYSTEM_ALERT_WINDOW\">"))
        } else {
            RuleResult.Pass
        }
}

object BackgroundLocationRequestedRule : AnalysisRule {
    override val id = "BACKGROUND_LOCATION_REQUESTED"
    override val metadata =
        RuleMetadata(
            title = "Background location access requested",
            category = FindingCategory.PERMISSIONS,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Confirm background location tracking is essential to the app's core function and disclosed to users.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult =
        if (facts.hasPermission("android.permission.ACCESS_BACKGROUND_LOCATION")) {
            RuleResult.Finding(evidence = listOf("<uses-permission android:name=\"android.permission.ACCESS_BACKGROUND_LOCATION\">"))
        } else {
            RuleResult.Pass
        }
}

object SmsOrCallLogPermissionRule : AnalysisRule {
    override val id = "SMS_OR_CALL_LOG_PERMISSION"
    override val metadata =
        RuleMetadata(
            title = "SMS or call log access requested",
            category = FindingCategory.PERMISSIONS,
            defaultSeverity = Severity.HIGH,
            remediation = "SMS/call-log permissions are highly restricted by Play policy; confirm this app qualifies for an allowed use case (e.g. default SMS/dialer handler).",
        )

    private val watched =
        setOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val matched = facts.manifest.permissions.filter { it.name in watched }
        return if (matched.isEmpty()) {
            RuleResult.Pass
        } else {
            RuleResult.Finding(evidence = matched.map { "<uses-permission android:name=\"${it.name}\">" })
        }
    }
}

object SensitivePermissionCombinationRule : AnalysisRule {
    override val id = "SENSITIVE_PERMISSION_COMBINATION"
    override val metadata =
        RuleMetadata(
            title = "Sensitive permission combination requested",
            category = FindingCategory.PERMISSIONS,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Camera, microphone, and background location together form a highly sensitive surveillance-capable combination; confirm each is independently justified.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val hasCamera = facts.hasPermission("android.permission.CAMERA")
        val hasMic = facts.hasPermission("android.permission.RECORD_AUDIO")
        val hasBackgroundLocation = facts.hasPermission("android.permission.ACCESS_BACKGROUND_LOCATION")
        return if (hasCamera && hasMic && hasBackgroundLocation) {
            RuleResult.Finding(
                evidence =
                    listOf(
                        "android.permission.CAMERA",
                        "android.permission.RECORD_AUDIO",
                        "android.permission.ACCESS_BACKGROUND_LOCATION",
                    ),
            )
        } else {
            RuleResult.Pass
        }
    }
}
