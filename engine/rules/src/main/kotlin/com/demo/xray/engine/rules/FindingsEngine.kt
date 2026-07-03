package com.demo.xray.engine.rules

import com.demo.xray.core.model.AnalysisFacts
import com.demo.xray.core.model.Finding

/** Rule-set version. Bump whenever a rule's logic or default severity changes. See PRD FR-114. */
const val RULE_SET_VERSION = "1.0.0"

interface FindingsEngine {
    fun evaluate(facts: AnalysisFacts, rules: List<AnalysisRule> = RuleRegistry.all): List<Finding>
}

class DefaultFindingsEngine : FindingsEngine {
    override fun evaluate(facts: AnalysisFacts, rules: List<AnalysisRule>): List<Finding> =
        rules.mapNotNull { rule ->
            when (val result = rule.evaluate(facts)) {
                is RuleResult.Finding ->
                    Finding(
                        ruleId = rule.id,
                        title = rule.metadata.title,
                        category = rule.metadata.category,
                        severity = result.severityOverride ?: rule.metadata.defaultSeverity,
                        confidence = result.confidence,
                        explanation = rule.metadata.title,
                        evidence = result.evidence,
                        affected = result.affected,
                        remediation = rule.metadata.remediation,
                        ruleSetVersion = RULE_SET_VERSION,
                    )
                RuleResult.Pass, RuleResult.NotApplicable, is RuleResult.Unknown -> null
            }
        }
}
