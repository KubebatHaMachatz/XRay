package com.demo.xray.engine.apk.manifest

import com.demo.xray.core.model.AnalysisOutcome
import com.demo.xray.core.model.ComponentType
import com.demo.xray.core.model.ExportedState
import com.demo.xray.engine.apk.axml.ANDROID_NAMESPACE
import com.demo.xray.engine.apk.axml.AxmlEncoder
import com.demo.xray.engine.apk.axml.AxmlParser
import com.demo.xray.engine.apk.axml.AxmlValueType
import com.demo.xray.engine.apk.axml.TestAttr
import com.demo.xray.engine.apk.axml.TestElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestAnalyzerTest {
    private fun decode(root: TestElement) = ManifestAnalyzer.analyze((AxmlParser.parse(AxmlEncoder.encode(root)) as AnalysisOutcome.Success).value)

    @Test
    fun `parses package version and uses-sdk`() {
        val manifest =
            TestElement(
                null,
                "manifest",
                attrs =
                    listOf(
                        TestAttr(null, "package", AxmlValueType.TYPE_STRING, stringValue = "com.example.app"),
                        TestAttr(ANDROID_NAMESPACE, "versionName", AxmlValueType.TYPE_STRING, stringValue = "2.0"),
                        TestAttr(ANDROID_NAMESPACE, "versionCode", AxmlValueType.TYPE_INT_DEC, data = 7),
                    ),
                children =
                    listOf(
                        TestElement(
                            null,
                            "uses-sdk",
                            attrs =
                                listOf(
                                    TestAttr(ANDROID_NAMESPACE, "minSdkVersion", AxmlValueType.TYPE_INT_DEC, data = 26),
                                    TestAttr(ANDROID_NAMESPACE, "targetSdkVersion", AxmlValueType.TYPE_INT_DEC, data = 34),
                                ),
                        ),
                    ),
            )

        val facts = decode(manifest)
        assertEquals("com.example.app", facts.packageName)
        assertEquals("2.0", facts.versionName)
        assertEquals(7L, facts.versionCode)
        assertEquals(26, facts.usesSdk.minSdkVersion)
        assertEquals(34, facts.usesSdk.targetSdkVersion)
    }

    @Test
    fun `parses requested permissions with known risk category`() {
        val manifest =
            TestElement(
                null,
                "manifest",
                children =
                    listOf(
                        TestElement(null, "uses-permission", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = "android.permission.CAMERA"))),
                        TestElement(null, "uses-permission", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = "com.example.CUSTOM_PERM"))),
                    ),
            )

        val facts = decode(manifest)
        assertEquals(2, facts.permissions.size)
        val camera = facts.permissions.first { it.name == "android.permission.CAMERA" }
        assertEquals(com.demo.xray.core.model.PermissionRiskCategory.SENSITIVE, camera.riskCategory)
        assertFalse(camera.isCustom)
        val custom = facts.permissions.first { it.name == "com.example.CUSTOM_PERM" }
        assertTrue(custom.isCustom)
        assertEquals(com.demo.xray.core.model.PermissionRiskCategory.UNKNOWN, custom.riskCategory)
    }

    @Test
    fun `parses application flags`() {
        val manifest =
            TestElement(
                null,
                "manifest",
                children =
                    listOf(
                        TestElement(
                            null,
                            "application",
                            attrs =
                                listOf(
                                    TestAttr(ANDROID_NAMESPACE, "debuggable", AxmlValueType.TYPE_INT_BOOLEAN, data = 1),
                                    TestAttr(ANDROID_NAMESPACE, "testOnly", AxmlValueType.TYPE_INT_BOOLEAN, data = 1),
                                    TestAttr(ANDROID_NAMESPACE, "usesCleartextTraffic", AxmlValueType.TYPE_INT_BOOLEAN, data = 1),
                                    TestAttr(ANDROID_NAMESPACE, "allowBackup", AxmlValueType.TYPE_INT_BOOLEAN, data = 0),
                                ),
                        ),
                    ),
            )

        val facts = decode(manifest)
        assertTrue(facts.applicationFlags.debuggable)
        assertTrue(facts.applicationFlags.testOnly)
        assertEquals(true, facts.applicationFlags.usesCleartextTraffic)
        assertEquals(false, facts.applicationFlags.allowBackup)
    }

    @Test
    fun `computes effective exported state for an activity with launcher intent-filter`() {
        val activity =
            TestElement(
                null,
                "activity",
                attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = ".MainActivity")),
                children =
                    listOf(
                        TestElement(
                            null,
                            "intent-filter",
                            children =
                                listOf(
                                    TestElement(null, "action", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = "android.intent.action.MAIN"))),
                                    TestElement(null, "category", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = "android.intent.category.LAUNCHER"))),
                                ),
                        ),
                    ),
            )
        val manifest = TestElement(null, "manifest", children = listOf(TestElement(null, "application", children = listOf(activity))))

        val facts = decode(manifest)
        val main = facts.components.single { it.type == ComponentType.ACTIVITY }
        assertEquals(ExportedState.EXPORTED, main.effectiveExported)
        assertTrue(main.isLauncher)
        assertEquals(null, main.explicitExported)
    }

    @Test
    fun `computes effective exported state for a provider using legacy default`() {
        val provider =
            TestElement(
                null,
                "provider",
                attrs =
                    listOf(
                        TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = ".MyProvider"),
                        TestAttr(ANDROID_NAMESPACE, "authorities", AxmlValueType.TYPE_STRING, stringValue = "com.example.app.provider"),
                    ),
            )
        val manifest =
            TestElement(
                null,
                "manifest",
                children =
                    listOf(
                        TestElement(null, "uses-sdk", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "targetSdkVersion", AxmlValueType.TYPE_INT_DEC, data = 34))),
                        TestElement(null, "application", children = listOf(provider)),
                    ),
            )

        val facts = decode(manifest)
        val p = facts.components.single { it.type == ComponentType.PROVIDER }
        assertEquals(ExportedState.NOT_EXPORTED, p.effectiveExported)
        assertEquals(listOf("com.example.app.provider"), p.authorities)
    }

    @Test
    fun `missing application element does not throw and produces a warning`() {
        val facts = decode(TestElement(null, "manifest"))
        assertTrue(facts.components.isEmpty())
        assertTrue(facts.warnings.any { it.contains("application", ignoreCase = true) })
    }
}
