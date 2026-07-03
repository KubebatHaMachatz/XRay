package com.demo.xray.engine.rules

import com.demo.xray.core.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindingsEngineTest {
    private val engine = DefaultFindingsEngine()

    private val alwaysFindingRule =
        object : AnalysisRule {
            override val id = "TEST_ALWAYS_FINDING"
            override val metadata = RuleMetadata("Always finds", com.demo.xray.core.model.FindingCategory.PACKAGING, Severity.LOW, "fix it")

            override fun evaluate(facts: com.demo.xray.core.model.AnalysisFacts): RuleResult = RuleResult.Finding(evidence = listOf("evidence-1"), severityOverride = Severity.CRITICAL)
        }

    private val alwaysPassRule =
        object : AnalysisRule {
            override val id = "TEST_ALWAYS_PASS"
            override val metadata = RuleMetadata("Never finds", com.demo.xray.core.model.FindingCategory.PACKAGING, Severity.LOW, "n/a")

            override fun evaluate(facts: com.demo.xray.core.model.AnalysisFacts): RuleResult = RuleResult.Pass
        }

    private val alwaysUnknownRule =
        object : AnalysisRule {
            override val id = "TEST_ALWAYS_UNKNOWN"
            override val metadata = RuleMetadata("Unknown", com.demo.xray.core.model.FindingCategory.PACKAGING, Severity.LOW, "n/a")

            override fun evaluate(facts: com.demo.xray.core.model.AnalysisFacts): RuleResult = RuleResult.Unknown("missing fact")
        }

    @Test
    fun `only Finding results become findings, severity override respected`() {
        val findings = engine.evaluate(baseFacts(), listOf(alwaysFindingRule, alwaysPassRule, alwaysUnknownRule))
        assertEquals(1, findings.size)
        assertEquals("TEST_ALWAYS_FINDING", findings.single().ruleId)
        assertEquals(Severity.CRITICAL, findings.single().severity)
        assertEquals(listOf("evidence-1"), findings.single().evidence)
        assertEquals(RULE_SET_VERSION, findings.single().ruleSetVersion)
    }

    @Test
    fun `default rule registry evaluates without throwing on minimal facts`() {
        val findings = engine.evaluate(baseFacts())
        assertTrue(findings.all { it.ruleSetVersion == RULE_SET_VERSION })
    }
}
