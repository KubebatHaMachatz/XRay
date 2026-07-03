package com.demo.xray.engine.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleRegistryTest {
    @Test
    fun `all rule IDs are unique`() {
        val ids = RuleRegistry.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every rule has a non-blank title and remediation`() {
        RuleRegistry.all.forEach { rule ->
            assertTrue("${rule.id} title", rule.metadata.title.isNotBlank())
            assertTrue("${rule.id} remediation", rule.metadata.remediation.isNotBlank())
        }
    }

    @Test
    fun `registry contains the documented FR-112 subset implemented in this milestone`() {
        val expected =
            setOf(
                "APP_DEBUGGABLE", "APP_TEST_ONLY", "CLEARTEXT_TRAFFIC_ALLOWED",
                "EXPORTED_PROVIDER_UNPROTECTED", "EXPORTED_SERVICE_UNPROTECTED", "EXPORTED_RECEIVER_UNPROTECTED",
                "EXPORTED_ACTIVITY_REVIEW", "WEAK_CUSTOM_PERMISSION_GUARD", "SHARED_USER_ID_DECLARED",
                "BACKUP_CONFIGURATION_REVIEW", "LEGACY_EXTERNAL_STORAGE", "QUERY_ALL_PACKAGES_REQUESTED",
                "REQUEST_INSTALL_PACKAGES", "SYSTEM_ALERT_WINDOW_REQUESTED", "ACCESSIBILITY_SERVICE_DECLARED",
                "BACKGROUND_LOCATION_REQUESTED", "SMS_OR_CALL_LOG_PERMISSION", "SENSITIVE_PERMISSION_COMBINATION",
                "TARGET_SDK_BELOW_POLICY", "ONLY_32_BIT_NATIVE_LIBS", "NATIVE_LIBRARY_ABI_MISMATCH",
                "LARGE_UNCOMPRESSED_ENTRY", "EXTREME_ZIP_EXPANSION_RATIO", "DUPLICATE_CRITICAL_ENTRY",
            )
        assertEquals(expected, RuleRegistry.all.map { it.id }.toSet())
    }
}
