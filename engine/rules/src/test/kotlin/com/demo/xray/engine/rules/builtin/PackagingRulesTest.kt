package com.demo.xray.engine.rules.builtin

import com.demo.xray.core.common.AnalysisLimits
import com.demo.xray.core.model.ArchiveEntryCategory
import com.demo.xray.engine.rules.RuleResult
import com.demo.xray.engine.rules.archiveEntry
import com.demo.xray.engine.rules.baseArchive
import com.demo.xray.engine.rules.baseFacts
import org.junit.Assert.assertTrue
import org.junit.Test

class PackagingRulesTest {
    @Test
    fun `ONLY_32_BIT_NATIVE_LIBS triggers when no 64-bit ABI present`() {
        val archive =
            baseArchive(
                entries =
                    listOf(
                        archiveEntry("lib/armeabi-v7a/libfoo.so", category = ArchiveEntryCategory.NATIVE_LIBRARY),
                    ),
            )
        assertTrue(Only32BitNativeLibsRule.evaluate(baseFacts(archive = archive)) is RuleResult.Finding)
    }

    @Test
    fun `ONLY_32_BIT_NATIVE_LIBS passes when 64-bit ABI present`() {
        val archive =
            baseArchive(
                entries =
                    listOf(
                        archiveEntry("lib/armeabi-v7a/libfoo.so", category = ArchiveEntryCategory.NATIVE_LIBRARY),
                        archiveEntry("lib/arm64-v8a/libfoo.so", category = ArchiveEntryCategory.NATIVE_LIBRARY),
                    ),
            )
        assertTrue(Only32BitNativeLibsRule.evaluate(baseFacts(archive = archive)) is RuleResult.Pass)
    }

    @Test
    fun `ONLY_32_BIT_NATIVE_LIBS is not applicable with no native libs`() {
        assertTrue(Only32BitNativeLibsRule.evaluate(baseFacts()) is RuleResult.NotApplicable)
    }

    @Test
    fun `NATIVE_LIBRARY_ABI_MISMATCH triggers when a lib is missing from one ABI`() {
        val archive =
            baseArchive(
                entries =
                    listOf(
                        archiveEntry("lib/armeabi-v7a/libfoo.so", category = ArchiveEntryCategory.NATIVE_LIBRARY),
                        archiveEntry("lib/arm64-v8a/libfoo.so", category = ArchiveEntryCategory.NATIVE_LIBRARY),
                        archiveEntry("lib/arm64-v8a/libbar.so", category = ArchiveEntryCategory.NATIVE_LIBRARY),
                    ),
            )
        assertTrue(NativeLibraryAbiMismatchRule.evaluate(baseFacts(archive = archive)) is RuleResult.Finding)
    }

    @Test
    fun `NATIVE_LIBRARY_ABI_MISMATCH passes when consistent`() {
        val archive =
            baseArchive(
                entries =
                    listOf(
                        archiveEntry("lib/armeabi-v7a/libfoo.so", category = ArchiveEntryCategory.NATIVE_LIBRARY),
                        archiveEntry("lib/arm64-v8a/libfoo.so", category = ArchiveEntryCategory.NATIVE_LIBRARY),
                    ),
            )
        assertTrue(NativeLibraryAbiMismatchRule.evaluate(baseFacts(archive = archive)) is RuleResult.Pass)
    }

    @Test
    fun `LARGE_UNCOMPRESSED_ENTRY triggers above threshold`() {
        val archive = baseArchive(entries = listOf(archiveEntry("assets/big.bin", uncompressedSize = AnalysisLimits.LARGE_UNCOMPRESSED_ENTRY_BYTES + 1)))
        assertTrue(LargeUncompressedEntryRule.evaluate(baseFacts(archive = archive)) is RuleResult.Finding)
    }

    @Test
    fun `LARGE_UNCOMPRESSED_ENTRY passes below threshold`() {
        val archive = baseArchive(entries = listOf(archiveEntry("assets/small.bin", uncompressedSize = 10)))
        assertTrue(LargeUncompressedEntryRule.evaluate(baseFacts(archive = archive)) is RuleResult.Pass)
    }

    @Test
    fun `EXTREME_ZIP_EXPANSION_RATIO reflects archive flag`() {
        val flagged = baseArchive().copy(expansionRatioExceedsPolicy = true)
        assertTrue(ExtremeZipExpansionRatioRule.evaluate(baseFacts(archive = flagged)) is RuleResult.Finding)
        assertTrue(ExtremeZipExpansionRatioRule.evaluate(baseFacts()) is RuleResult.Pass)
    }

    @Test
    fun `DUPLICATE_CRITICAL_ENTRY reflects archive duplicate list`() {
        val flagged = baseArchive().copy(duplicateCriticalEntryNames = listOf("classes.dex"))
        assertTrue(DuplicateCriticalEntryRule.evaluate(baseFacts(archive = flagged)) is RuleResult.Finding)
        assertTrue(DuplicateCriticalEntryRule.evaluate(baseFacts()) is RuleResult.Pass)
    }
}
