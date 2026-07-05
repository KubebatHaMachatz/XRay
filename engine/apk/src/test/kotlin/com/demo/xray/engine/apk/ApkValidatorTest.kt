package com.demo.xray.engine.apk

import com.demo.xray.core.model.AnalysisError
import com.demo.xray.core.model.AnalysisOutcome
import com.demo.xray.core.model.ArchiveEntryCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkValidatorTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val validator = DefaultApkValidator()

    private fun fileOf(bytes: ByteArray): java.io.File {
        val f = tempFolder.newFile()
        f.writeBytes(bytes)
        return f
    }

    @Test
    fun `valid apk-like zip is accepted with manifest and dex categorized`() {
        val result = validator.validate(fileOf(minimalApkBytes()))
        val inventory = (result as AnalysisOutcome.Success).value
        val manifestEntry = inventory.entries.first { it.path == ANDROID_MANIFEST_ENTRY_NAME }
        assertEquals(ArchiveEntryCategory.COMPILED_MANIFEST, manifestEntry.category)
        val dexEntry = inventory.entries.first { it.path == "classes.dex" }
        assertEquals(ArchiveEntryCategory.DEX, dexEntry.category)
    }

    @Test
    fun `invalid zip magic is rejected`() {
        val result = validator.validate(fileOf(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)))
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.InvalidZip)
    }

    @Test
    fun `missing manifest is rejected`() {
        val bytes = buildZip("res/values.xml" to ByteArray(4))
        val result = validator.validate(fileOf(bytes))
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.MissingManifest)
    }

    @Test
    fun `duplicate manifest entries are rejected as ambiguous`() {
        val bytes =
            buildRawZipAllowingDuplicates(
                listOf(
                    ANDROID_MANIFEST_ENTRY_NAME to byteArrayOf(1),
                    ANDROID_MANIFEST_ENTRY_NAME to byteArrayOf(2),
                ),
            )
        val result = validator.validate(fileOf(bytes))
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.InvalidZip)
    }

    @Test
    fun `too many entries is rejected`() {
        val out = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry(ANDROID_MANIFEST_ENTRY_NAME))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
            repeat(100_001) { i ->
                zip.putNextEntry(java.util.zip.ZipEntry("f$i.txt"))
                zip.closeEntry()
            }
        }
        val result = validator.validate(fileOf(out.toByteArray()))
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.TooManyEntries)
    }

    @Test
    fun `path traversal entry names are flagged as unsafe warnings`() {
        val bytes = buildZip(ANDROID_MANIFEST_ENTRY_NAME to byteArrayOf(1), "../../etc/passwd" to byteArrayOf(2))
        val result = validator.validate(fileOf(bytes))
        val inventory = (result as AnalysisOutcome.Success).value
        assertTrue(inventory.warnings.any { it.contains("unsafe") })
    }

    @Test
    fun `empty file is unreadable`() {
        val result = validator.validate(fileOf(ByteArray(0)))
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.UnreadableSource)
    }

    @Test
    fun `categorizes native libraries assets and signing metadata`() {
        val bytes =
            buildZip(
                ANDROID_MANIFEST_ENTRY_NAME to byteArrayOf(1),
                "lib/arm64-v8a/libfoo.so" to ByteArray(8),
                "assets/data.json" to ByteArray(8),
                "META-INF/CERT.RSA" to ByteArray(8),
                "META-INF/MANIFEST.MF" to ByteArray(8),
                "resources.arsc" to ByteArray(8),
            )
        val inventory = (validator.validate(fileOf(bytes)) as AnalysisOutcome.Success).value
        fun categoryOf(path: String) = inventory.entries.first { it.path == path }.category
        assertEquals(ArchiveEntryCategory.NATIVE_LIBRARY, categoryOf("lib/arm64-v8a/libfoo.so"))
        assertEquals(ArchiveEntryCategory.ASSETS, categoryOf("assets/data.json"))
        assertEquals(ArchiveEntryCategory.SIGNING_METADATA, categoryOf("META-INF/CERT.RSA"))
        assertEquals(ArchiveEntryCategory.SIGNING_METADATA, categoryOf("META-INF/MANIFEST.MF"))
        assertEquals(ArchiveEntryCategory.RESOURCES, categoryOf("resources.arsc"))
    }
}
