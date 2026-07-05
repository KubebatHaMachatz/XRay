package com.demo.xray.engine.rules

import com.demo.xray.core.model.AnalysisFacts
import com.demo.xray.core.model.Confidence
import com.demo.xray.core.model.FindingCategory
import com.demo.xray.core.model.Severity

/**
 * Metadata about a rule (title, category, default severity, remediation advice).
 *
 * Kept separate from the rule logic so it can be displayed in UIs or documentation
 * without necessarily running the rule evaluation.
 */
data class RuleMetadata(
    /** Short, user-visible title (e.g., "Debuggable application") */
    val title: String,

    /** Category for organizing findings (SIGNING, MANIFEST_CONFIGURATION, etc.) */
    val category: FindingCategory,

    /** Default severity if the rule triggers (can be overridden per-finding via severityOverride) */
    val defaultSeverity: Severity,

    /** Developer-facing advice on how to remediate the issue */
    val remediation: String,
)

/**
 * Result of evaluating a single rule against an APK's facts.
 *
 * A rule must be explicit about whether data is missing (Unknown) rather than silently
 * guessing. This supports auditing (the log will show "rule X was skipped because Y was
 * unavailable") and prevents false positives from incomplete analysis.
 */
sealed class RuleResult {
    /**
     * Rule evaluated and found no issue. The component/permission/etc. is secure according
     * to this rule. The UI shows this as a pass (or omits it entirely if configured).
     */
    data object Pass : RuleResult()

    /**
     * Rule found a security issue.
     *
     * @property evidence List of specific facts that triggered the rule (e.g.,
     *   ["Permission: android.permission.CAMERA", "Used by activity: MainActivity"])
     * @property affected Optional: the specific component, permission, or asset affected
     * @property severityOverride Optional: override the rule's default severity for this finding
     *   (e.g., a generally MEDIUM rule becomes CRITICAL if it detects a specific pattern)
     * @property confidence How confident the engine is in this detection (defaults to HIGH)
     */
    data class Finding(
        val evidence: List<String>,
        val affected: String? = null,
        val severityOverride: Severity? = null,
        val confidence: Confidence = Confidence.HIGH,
    ) : RuleResult()

    /**
     * Rule does not apply to this APK (e.g., "no providers defined" for a provider-specific rule).
     * The UI does not count this as a finding; it's informational only.
     */
    data object NotApplicable : RuleResult()

    /**
     * Rule cannot be evaluated because a required fact is missing or unavailable.
     *
     * Examples:
     *  - "targetSdkVersion unknown" (can't compute export state)
     *  - "DEX analysis deferred" (SDK-detection rules not yet implemented)
     *
     * This is important for audit trails: users can see which analyses were incomplete.
     */
    data class Unknown(val reason: String) : RuleResult()
}

/**
 * Contract for a single security/best-practice rule.
 *
 * Design:
 *  - Rules are immutable, side-effect-free functions ([evaluate] is pure)
 *  - [id] must remain stable across rule-set versions once shipped (see RULES.md for versioning)
 *  - Rules work only from [AnalysisFacts]; no access to the filesystem or Android framework
 *    (makes them testable on the JVM)
 *  - Each rule is responsible for deciding whether it applies, what severity to assign,
 *    and what evidence to collect (context-sensitive judgment)
 *
 * The rules engine (DefaultFindingsEngine) calls evaluate() on every rule and collects findings.
 * See builtin/ for 24+ example rule implementations.
 */
interface AnalysisRule {
    /** Stable, unique identifier (e.g., "EXPORTED_PROVIDER_UNPROTECTED"). Used in logs and documentation. */
    val id: String

    /** Metadata for display and documentation. */
    val metadata: RuleMetadata

    /** Evaluate this rule against the APK's facts and return a result. Must be a pure function. */
    fun evaluate(facts: AnalysisFacts): RuleResult
}
