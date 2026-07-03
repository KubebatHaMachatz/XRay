package com.demo.xray.engine.rules.builtin

import com.demo.xray.core.model.ComponentType
import com.demo.xray.core.model.CustomPermissionFact
import com.demo.xray.core.model.ExportedState
import com.demo.xray.core.model.IntentFilterFact
import com.demo.xray.engine.rules.RuleResult
import com.demo.xray.engine.rules.baseFacts
import com.demo.xray.engine.rules.baseManifest
import com.demo.xray.engine.rules.component
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentExposureRulesTest {
    @Test
    fun `EXPORTED_PROVIDER_UNPROTECTED triggers when no permission fields set`() {
        val facts = baseFacts(manifest = baseManifest(components = listOf(component("com.example.P", ComponentType.PROVIDER, effectiveExported = ExportedState.EXPORTED))))
        assertTrue(ExportedProviderUnprotectedRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `EXPORTED_PROVIDER_UNPROTECTED passes when readPermission is set`() {
        val facts =
            baseFacts(
                manifest =
                    baseManifest(
                        components = listOf(component("com.example.P", ComponentType.PROVIDER, effectiveExported = ExportedState.EXPORTED, readPermission = "com.example.READ")),
                    ),
            )
        assertTrue(ExportedProviderUnprotectedRule.evaluate(facts) is RuleResult.Pass)
    }

    @Test
    fun `EXPORTED_PROVIDER_UNPROTECTED passes when not exported`() {
        val facts = baseFacts(manifest = baseManifest(components = listOf(component("com.example.P", ComponentType.PROVIDER))))
        assertTrue(ExportedProviderUnprotectedRule.evaluate(facts) is RuleResult.Pass)
    }

    @Test
    fun `EXPORTED_SERVICE_UNPROTECTED triggers and passes appropriately`() {
        val exposed = baseFacts(manifest = baseManifest(components = listOf(component(".S", ComponentType.SERVICE, effectiveExported = ExportedState.EXPORTED))))
        assertTrue(ExportedServiceUnprotectedRule.evaluate(exposed) is RuleResult.Finding)

        val guarded =
            baseFacts(
                manifest = baseManifest(components = listOf(component(".S", ComponentType.SERVICE, effectiveExported = ExportedState.EXPORTED, permission = "com.example.BIND"))),
            )
        assertTrue(ExportedServiceUnprotectedRule.evaluate(guarded) is RuleResult.Pass)
    }

    @Test
    fun `EXPORTED_RECEIVER_UNPROTECTED triggers when unguarded and exported`() {
        val facts = baseFacts(manifest = baseManifest(components = listOf(component(".R", ComponentType.RECEIVER, effectiveExported = ExportedState.EXPORTED))))
        assertTrue(ExportedReceiverUnprotectedRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `EXPORTED_ACTIVITY_REVIEW ignores launcher activities`() {
        val launcher =
            baseFacts(
                manifest = baseManifest(components = listOf(component(".Main", ComponentType.ACTIVITY, effectiveExported = ExportedState.EXPORTED, isLauncher = true))),
            )
        assertTrue(ExportedActivityReviewRule.evaluate(launcher) is RuleResult.Pass)

        val deepLink =
            baseFacts(
                manifest = baseManifest(components = listOf(component(".Deep", ComponentType.ACTIVITY, effectiveExported = ExportedState.EXPORTED, isLauncher = false))),
            )
        assertTrue(ExportedActivityReviewRule.evaluate(deepLink) is RuleResult.Finding)
    }

    @Test
    fun `WEAK_CUSTOM_PERMISSION_GUARD triggers for normal protection level and passes for signature`() {
        val weakManifest =
            baseManifest(components = listOf(component(".S", ComponentType.SERVICE, effectiveExported = ExportedState.EXPORTED, permission = "com.example.GUARD")))
                .copy(customPermissions = listOf(CustomPermissionFact("com.example.GUARD", protectionLevel = "normal", label = null, description = null)))
        assertTrue(WeakCustomPermissionGuardRule.evaluate(baseFacts(manifest = weakManifest)) is RuleResult.Finding)

        val strongManifest =
            baseManifest(components = listOf(component(".S", ComponentType.SERVICE, effectiveExported = ExportedState.EXPORTED, permission = "com.example.GUARD")))
                .copy(customPermissions = listOf(CustomPermissionFact("com.example.GUARD", protectionLevel = "signature", label = null, description = null)))
        assertTrue(WeakCustomPermissionGuardRule.evaluate(baseFacts(manifest = strongManifest)) is RuleResult.Pass)
    }

    @Test
    fun `ACCESSIBILITY_SERVICE_DECLARED triggers only for accessibility action`() {
        val filter = IntentFilterFact(actions = listOf("android.accessibilityservice.AccessibilityService"))
        val facts = baseFacts(manifest = baseManifest(components = listOf(component(".A11y", ComponentType.SERVICE, intentFilters = listOf(filter)))))
        assertTrue(AccessibilityServiceDeclaredRule.evaluate(facts) is RuleResult.Finding)

        val unrelated = baseFacts(manifest = baseManifest(components = listOf(component(".Other", ComponentType.SERVICE))))
        assertTrue(AccessibilityServiceDeclaredRule.evaluate(unrelated) is RuleResult.Pass)
    }
}
