package com.demo.xray.engine.rules.builtin

import com.demo.xray.engine.rules.RuleResult
import com.demo.xray.engine.rules.baseFacts
import com.demo.xray.engine.rules.baseManifest
import com.demo.xray.engine.rules.permission
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionRulesTest {
    @Test
    fun `QUERY_ALL_PACKAGES_REQUESTED triggers only when permission present`() {
        val facts = baseFacts(manifest = baseManifest(permissions = listOf(permission("android.permission.QUERY_ALL_PACKAGES"))).copy(queriesAllPackages = true))
        assertTrue(QueryAllPackagesRequestedRule.evaluate(facts) is RuleResult.Finding)
        assertTrue(QueryAllPackagesRequestedRule.evaluate(baseFacts()) is RuleResult.Pass)
    }

    @Test
    fun `REQUEST_INSTALL_PACKAGES triggers when requested`() {
        val facts = baseFacts(manifest = baseManifest(permissions = listOf(permission("android.permission.REQUEST_INSTALL_PACKAGES"))))
        assertTrue(RequestInstallPackagesRule.evaluate(facts) is RuleResult.Finding)
        assertTrue(RequestInstallPackagesRule.evaluate(baseFacts()) is RuleResult.Pass)
    }

    @Test
    fun `SYSTEM_ALERT_WINDOW_REQUESTED triggers when requested`() {
        val facts = baseFacts(manifest = baseManifest(permissions = listOf(permission("android.permission.SYSTEM_ALERT_WINDOW"))))
        assertTrue(SystemAlertWindowRequestedRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `BACKGROUND_LOCATION_REQUESTED triggers when requested`() {
        val facts = baseFacts(manifest = baseManifest(permissions = listOf(permission("android.permission.ACCESS_BACKGROUND_LOCATION"))))
        assertTrue(BackgroundLocationRequestedRule.evaluate(facts) is RuleResult.Finding)
    }

    @Test
    fun `SMS_OR_CALL_LOG_PERMISSION triggers for any watched permission`() {
        val facts = baseFacts(manifest = baseManifest(permissions = listOf(permission("android.permission.READ_SMS"))))
        assertTrue(SmsOrCallLogPermissionRule.evaluate(facts) is RuleResult.Finding)
        assertTrue(SmsOrCallLogPermissionRule.evaluate(baseFacts()) is RuleResult.Pass)
    }

    @Test
    fun `SENSITIVE_PERMISSION_COMBINATION requires all three permissions`() {
        val partial =
            baseFacts(
                manifest = baseManifest(permissions = listOf(permission("android.permission.CAMERA"), permission("android.permission.RECORD_AUDIO"))),
            )
        assertTrue(SensitivePermissionCombinationRule.evaluate(partial) is RuleResult.Pass)

        val full =
            baseFacts(
                manifest =
                    baseManifest(
                        permissions =
                            listOf(
                                permission("android.permission.CAMERA"),
                                permission("android.permission.RECORD_AUDIO"),
                                permission("android.permission.ACCESS_BACKGROUND_LOCATION"),
                            ),
                    ),
            )
        assertTrue(SensitivePermissionCombinationRule.evaluate(full) is RuleResult.Finding)
    }
}
