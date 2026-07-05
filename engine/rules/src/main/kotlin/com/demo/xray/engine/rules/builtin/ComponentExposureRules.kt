package com.demo.xray.engine.rules.builtin

import com.demo.xray.core.model.AnalysisFacts
import com.demo.xray.core.model.ComponentFact
import com.demo.xray.core.model.ComponentType
import com.demo.xray.core.model.Confidence
import com.demo.xray.core.model.ExportedState
import com.demo.xray.core.model.FindingCategory
import com.demo.xray.core.model.Severity
import com.demo.xray.engine.rules.AnalysisRule
import com.demo.xray.engine.rules.RuleMetadata
import com.demo.xray.engine.rules.RuleResult

/**
 * Component exposure rules (FR-110/FR-111 in the PRD).
 *
 * These rules detect components (activities, services, receivers, providers) that are:
 *  1. Externally accessible (effectively exported)
 *  2. Not protected by a permission guard
 *
 * Why this matters:
 *  - An exported unprotected component is an attack surface; other apps can call it
 *  - A malicious app can inject data, launch sensitive screens, intercept broadcasts, or
 *    access the provider's data without the user's knowledge
 *  - Adding even a simple signature-level permission significantly raises the bar
 *
 * See AndroidSecurityBestPractices and CWE-927 (Improper Locking of Executable-Only-Format File).
 */

private const val ACCESSIBILITY_SERVICE_ACTION = "android.accessibilityservice.AccessibilityService"

/**
 * Helper: filter components by type.
 *
 * @param facts Analysis facts (contains all parsed manifest data)
 * @param type Component type to filter by
 * @return All components of the specified type
 */
private fun exportedComponentsOf(facts: AnalysisFacts, type: ComponentType): List<ComponentFact> =
    facts.manifest.components.filter { it.type == type }

/**
 * Helper: combine multiple unprotected-exported-component findings into one RuleResult.
 *
 * Reduces duplication: services, receivers, and activities share the same rule logic
 * ("if exported and no permission guard, flag it"), so this helper is reused.
 *
 * @param components All components of a given type
 * @param extraCheck Optional predicate for type-specific logic (e.g., "not a launcher activity")
 * @return Pass if no offenders, Finding if some are unprotected and exported
 */
private fun unprotectedFinding(components: List<ComponentFact>, extraCheck: (ComponentFact) -> Boolean): RuleResult {
    val offenders =
        components.filter {
            it.effectiveExported == ExportedState.EXPORTED && it.permission == null && extraCheck(it)
        }
    return if (offenders.isEmpty()) {
        RuleResult.Pass
    } else {
        RuleResult.Finding(
            evidence = offenders.map { "${it.name}: exported, no guarding permission (${it.exportedExplanation})" },
            affected = offenders.joinToString(", ") { it.name },
        )
    }
}

object ExportedProviderUnprotectedRule : AnalysisRule {
    override val id = "EXPORTED_PROVIDER_UNPROTECTED"
    override val metadata =
        RuleMetadata(
            title = "Exported provider lacks a guarding permission",
            category = FindingCategory.COMPONENT_EXPOSURE,
            defaultSeverity = Severity.HIGH,
            remediation = "Add android:permission, android:readPermission, or android:writePermission, or set android:exported=\"false\" if external access is not required.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val providers = exportedComponentsOf(facts, ComponentType.PROVIDER)
        val offenders =
            providers.filter {
                it.effectiveExported == ExportedState.EXPORTED &&
                    it.permission == null &&
                    it.readPermission == null &&
                    it.writePermission == null
            }
        return if (offenders.isEmpty()) {
            RuleResult.Pass
        } else {
            RuleResult.Finding(
                evidence = offenders.map { "${it.name}: exported provider with no permission/readPermission/writePermission" },
                affected = offenders.joinToString(", ") { it.name },
            )
        }
    }
}

object ExportedServiceUnprotectedRule : AnalysisRule {
    override val id = "EXPORTED_SERVICE_UNPROTECTED"
    override val metadata =
        RuleMetadata(
            title = "Exported service lacks a guarding permission",
            category = FindingCategory.COMPONENT_EXPOSURE,
            defaultSeverity = Severity.HIGH,
            remediation = "Add android:permission to restrict callers, or set android:exported=\"false\" if the service is not intended to be called by other apps.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult = unprotectedFinding(exportedComponentsOf(facts, ComponentType.SERVICE)) { true }
}

object ExportedReceiverUnprotectedRule : AnalysisRule {
    override val id = "EXPORTED_RECEIVER_UNPROTECTED"
    override val metadata =
        RuleMetadata(
            title = "Exported receiver lacks a guarding permission",
            category = FindingCategory.COMPONENT_EXPOSURE,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Add android:permission if this receiver should not accept broadcasts from arbitrary apps. Receivers for well-known public system broadcasts may be an intentional exception; review the intent-filter actions.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult = unprotectedFinding(exportedComponentsOf(facts, ComponentType.RECEIVER)) { true }
}

object ExportedActivityReviewRule : AnalysisRule {
    override val id = "EXPORTED_ACTIVITY_REVIEW"
    override val metadata =
        RuleMetadata(
            title = "Exported non-launcher activity should be reviewed",
            category = FindingCategory.COMPONENT_EXPOSURE,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Confirm this activity is intended to be externally invokable (e.g. a deep link target). Add android:permission if it should be restricted.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val activities = exportedComponentsOf(facts, ComponentType.ACTIVITY) + exportedComponentsOf(facts, ComponentType.ACTIVITY_ALIAS)
        val offenders =
            activities.filter {
                it.effectiveExported == ExportedState.EXPORTED && it.permission == null && !it.isLauncher
            }
        return if (offenders.isEmpty()) {
            RuleResult.Pass
        } else {
            RuleResult.Finding(
                evidence = offenders.map { "${it.name}: exported, no guarding permission, not a launcher activity" },
                affected = offenders.joinToString(", ") { it.name },
                confidence = Confidence.MEDIUM,
            )
        }
    }
}

object WeakCustomPermissionGuardRule : AnalysisRule {
    override val id = "WEAK_CUSTOM_PERMISSION_GUARD"
    override val metadata =
        RuleMetadata(
            title = "Exported component guarded only by a weak custom permission",
            category = FindingCategory.COMPONENT_EXPOSURE,
            defaultSeverity = Severity.MEDIUM,
            remediation = "Use a protectionLevel of signature (or stronger) for permissions that guard sensitive exported components.",
        )

    private val weakProtectionLevels = setOf(null, "normal", "dangerous")

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val customPermissionsByName = facts.manifest.customPermissions.associateBy { it.name }
        val offenders =
            facts.manifest.components.filter { component ->
                val permissionName = component.permission ?: return@filter false
                val custom = customPermissionsByName[permissionName] ?: return@filter false
                component.effectiveExported == ExportedState.EXPORTED && custom.protectionLevel in weakProtectionLevels
            }
        return if (offenders.isEmpty()) {
            RuleResult.Pass
        } else {
            RuleResult.Finding(
                evidence = offenders.map { "${it.name}: guarded by custom permission \"${it.permission}\" with protectionLevel=${customPermissionsByName[it.permission]?.protectionLevel ?: "normal"}" },
                affected = offenders.joinToString(", ") { it.name },
            )
        }
    }
}

object AccessibilityServiceDeclaredRule : AnalysisRule {
    override val id = "ACCESSIBILITY_SERVICE_DECLARED"
    override val metadata =
        RuleMetadata(
            title = "Accessibility service declared",
            category = FindingCategory.COMPONENT_EXPOSURE,
            defaultSeverity = Severity.HIGH,
            remediation = "Ensure the accessibility service's scope and permissions match its declared purpose; this is a high-impact capability that warrants review, not proof of abuse.",
        )

    override fun evaluate(facts: AnalysisFacts): RuleResult {
        val offenders =
            exportedComponentsOf(facts, ComponentType.SERVICE).filter { service ->
                service.intentFilters.any { it.actions.contains(ACCESSIBILITY_SERVICE_ACTION) }
            }
        return if (offenders.isEmpty()) {
            RuleResult.Pass
        } else {
            RuleResult.Finding(
                evidence = offenders.map { "${it.name}: declares intent-filter action $ACCESSIBILITY_SERVICE_ACTION" },
                affected = offenders.joinToString(", ") { it.name },
            )
        }
    }
}
