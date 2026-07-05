package com.demo.xray.engine.rules

import com.demo.xray.engine.rules.builtin.AccessibilityServiceDeclaredRule
import com.demo.xray.engine.rules.builtin.AppDebuggableRule
import com.demo.xray.engine.rules.builtin.AppTestOnlyRule
import com.demo.xray.engine.rules.builtin.BackgroundLocationRequestedRule
import com.demo.xray.engine.rules.builtin.BackupConfigurationReviewRule
import com.demo.xray.engine.rules.builtin.CleartextTrafficAllowedRule
import com.demo.xray.engine.rules.builtin.DuplicateCriticalEntryRule
import com.demo.xray.engine.rules.builtin.ExportedActivityReviewRule
import com.demo.xray.engine.rules.builtin.ExportedProviderUnprotectedRule
import com.demo.xray.engine.rules.builtin.ExportedReceiverUnprotectedRule
import com.demo.xray.engine.rules.builtin.ExportedServiceUnprotectedRule
import com.demo.xray.engine.rules.builtin.ExtremeZipExpansionRatioRule
import com.demo.xray.engine.rules.builtin.LargeUncompressedEntryRule
import com.demo.xray.engine.rules.builtin.LegacyExternalStorageRule
import com.demo.xray.engine.rules.builtin.NativeLibraryAbiMismatchRule
import com.demo.xray.engine.rules.builtin.Only32BitNativeLibsRule
import com.demo.xray.engine.rules.builtin.QueryAllPackagesRequestedRule
import com.demo.xray.engine.rules.builtin.RequestInstallPackagesRule
import com.demo.xray.engine.rules.builtin.SensitivePermissionCombinationRule
import com.demo.xray.engine.rules.builtin.SharedUserIdDeclaredRule
import com.demo.xray.engine.rules.builtin.SmsOrCallLogPermissionRule
import com.demo.xray.engine.rules.builtin.SystemAlertWindowRequestedRule
import com.demo.xray.engine.rules.builtin.TargetSdkBelowPolicyRule
import com.demo.xray.engine.rules.builtin.WeakCustomPermissionGuardRule

/** All built-in rules, in the order they should be evaluated/displayed. See PRD FR-112. */
object RuleRegistry {
    val all: List<AnalysisRule> =
        listOf(
            AppDebuggableRule,
            AppTestOnlyRule,
            CleartextTrafficAllowedRule,
            ExportedProviderUnprotectedRule,
            ExportedServiceUnprotectedRule,
            ExportedReceiverUnprotectedRule,
            ExportedActivityReviewRule,
            WeakCustomPermissionGuardRule,
            SharedUserIdDeclaredRule,
            BackupConfigurationReviewRule,
            LegacyExternalStorageRule,
            QueryAllPackagesRequestedRule,
            RequestInstallPackagesRule,
            SystemAlertWindowRequestedRule,
            AccessibilityServiceDeclaredRule,
            BackgroundLocationRequestedRule,
            SmsOrCallLogPermissionRule,
            SensitivePermissionCombinationRule,
            TargetSdkBelowPolicyRule,
            Only32BitNativeLibsRule,
            NativeLibraryAbiMismatchRule,
            LargeUncompressedEntryRule,
            ExtremeZipExpansionRatioRule,
            DuplicateCriticalEntryRule,
        )

    init {
        val duplicateIds = all.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        check(duplicateIds.isEmpty()) { "Duplicate rule IDs registered: $duplicateIds" }
    }
}
