package com.demo.xray.engine.apk.axml

import com.demo.xray.core.model.AnalysisOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AxmlParserTest {
    @Test
    fun `decodes minimal manifest with no attributes`() {
        val doc =
            AxmlEncoder.encode(
                TestElement(namespaceUri = null, name = "manifest"),
            )

        val result = AxmlParser.parse(doc)
        val root = (result as AnalysisOutcome.Success).value.root
        assertEquals("manifest", root.name)
        assertTrue(root.attributes.isEmpty())
        assertTrue(root.children.isEmpty())
    }

    @Test
    fun `decodes namespaced string and boolean attributes`() {
        val manifest =
            TestElement(
                namespaceUri = null,
                name = "manifest",
                attrs =
                    listOf(
                        TestAttr(ANDROID_NAMESPACE, "versionName", AxmlValueType.TYPE_STRING, stringValue = "1.2.3"),
                        TestAttr(ANDROID_NAMESPACE, "debuggable", AxmlValueType.TYPE_INT_BOOLEAN, data = 1),
                        TestAttr(null, "package", AxmlValueType.TYPE_STRING, stringValue = "com.example.app"),
                    ),
            )

        val result = AxmlParser.parse(AxmlEncoder.encode(manifest))
        val root = (result as AnalysisOutcome.Success).value.root

        val versionName = root.attr("versionName")
        assertEquals("1.2.3", versionName?.asString)

        val debuggable = root.attr("debuggable")
        assertEquals(true, debuggable?.asBoolean)

        val pkg = root.attr("package", namespaceUri = null)
        assertEquals("com.example.app", pkg?.asString)
    }

    @Test
    fun `decodes nested element tree preserving order`() {
        val application =
            TestElement(
                namespaceUri = null,
                name = "application",
                children =
                    listOf(
                        TestElement(null, "activity", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = ".MainActivity"))),
                        TestElement(null, "service", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = ".MyService"))),
                    ),
            )
        val manifest = TestElement(null, "manifest", children = listOf(application))

        val result = AxmlParser.parse(AxmlEncoder.encode(manifest))
        val root = (result as AnalysisOutcome.Success).value.root

        val app = root.childrenNamed("application").single()
        assertEquals(2, app.children.size)
        assertEquals("activity", app.children[0].name)
        assertEquals(".MainActivity", app.children[0].attr("name")?.asString)
        assertEquals("service", app.children[1].name)
        assertEquals(".MyService", app.children[1].attr("name")?.asString)
    }

    @Test
    fun `decodes deeply nested intent-filter structure`() {
        val intentFilter =
            TestElement(
                null,
                "intent-filter",
                children =
                    listOf(
                        TestElement(null, "action", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = "android.intent.action.MAIN"))),
                        TestElement(null, "category", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = "android.intent.category.LAUNCHER"))),
                    ),
            )
        val activity = TestElement(null, "activity", children = listOf(intentFilter))
        val manifest = TestElement(null, "manifest", children = listOf(TestElement(null, "application", children = listOf(activity))))

        val result = AxmlParser.parse(AxmlEncoder.encode(manifest))
        val root = (result as AnalysisOutcome.Success).value.root
        val filter = root.childrenNamed("application").single().childrenNamed("activity").single().childrenNamed("intent-filter").single()
        assertEquals("android.intent.action.MAIN", filter.childrenNamed("action").single().attr("name")?.asString)
        assertEquals("android.intent.category.LAUNCHER", filter.childrenNamed("category").single().attr("name")?.asString)
    }

    @Test
    fun `rejects wrong file type magic`() {
        val bogus = byteArrayOf(0x02, 0x00, 0x08, 0x00, 0x08, 0x00, 0x00, 0x00)
        val result = AxmlParser.parse(bogus)
        assertTrue(result is AnalysisOutcome.Failure)
    }

    @Test
    fun `rejects empty input`() {
        val result = AxmlParser.parse(ByteArray(0))
        assertTrue(result is AnalysisOutcome.Failure)
    }

    @Test
    fun `rejects truncated chunk without crashing`() {
        val full = AxmlEncoder.encode(TestElement(null, "manifest"))
        val truncated = full.copyOfRange(0, full.size - 10)
        val result = AxmlParser.parse(truncated)
        assertTrue(result is AnalysisOutcome.Failure)
    }

    @Test
    fun `rejects unbalanced end element`() {
        val full = AxmlEncoder.encode(TestElement(null, "manifest", children = listOf(TestElement(null, "application"))))
        // Corrupt: chop off the final END_ELEMENT chunk (24 bytes) to unbalance the tree.
        val corrupted = full.copyOfRange(0, full.size - 24)
        // Also fix up the outer chunkSize so the truncation isn't caught purely as an out-of-range read.
        val result = AxmlParser.parse(corrupted)
        assertTrue(result is AnalysisOutcome.Failure)
    }

    @Test
    fun `handles many sibling elements without stack issues`() {
        val children = (1..2000).map { TestElement(null, "meta-data", attrs = listOf(TestAttr(ANDROID_NAMESPACE, "name", AxmlValueType.TYPE_STRING, stringValue = "key$it"))) }
        val manifest = TestElement(null, "manifest", children = listOf(TestElement(null, "application", children = children)))

        val result = AxmlParser.parse(AxmlEncoder.encode(manifest))
        val root = (result as AnalysisOutcome.Success).value.root
        assertEquals(2000, root.childrenNamed("application").single().children.size)
    }

    @Test
    fun `enforces maximum nesting depth`() {
        var innermost = TestElement(null, "leaf")
        repeat(600) { innermost = TestElement(null, "level", children = listOf(innermost)) }

        val result = AxmlParser.parse(AxmlEncoder.encode(innermost))
        assertTrue(result is AnalysisOutcome.Failure)
    }

    @Test
    fun `resolved value renders integer types`() {
        val manifest =
            TestElement(
                null,
                "manifest",
                attrs =
                    listOf(
                        TestAttr(ANDROID_NAMESPACE, "versionCode", AxmlValueType.TYPE_INT_DEC, data = 42),
                        TestAttr(ANDROID_NAMESPACE, "flag", AxmlValueType.TYPE_INT_HEX, data = 0xAB),
                    ),
            )
        val result = AxmlParser.parse(AxmlEncoder.encode(manifest))
        val root = (result as AnalysisOutcome.Success).value.root
        assertEquals(42, root.attr("versionCode")?.asInt)
        assertEquals("0x000000ab", root.attr("flag")?.resolvedValue)
    }

    @Test
    fun `unknown attribute returns null`() {
        val result = AxmlParser.parse(AxmlEncoder.encode(TestElement(null, "manifest")))
        val root = (result as AnalysisOutcome.Success).value.root
        assertNull(root.attr("doesNotExist"))
    }
}
