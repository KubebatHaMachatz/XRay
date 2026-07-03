package com.demo.xray.engine.apk

import com.demo.xray.core.common.AnalysisLimits
import com.demo.xray.core.common.Sha256HashCalculator
import com.demo.xray.core.model.AnalysisError
import com.demo.xray.core.model.AnalysisOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.InputStream
import java.security.MessageDigest

class ApkImporterTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val importer = DefaultApkImporter(Sha256HashCalculator)

    @Test
    fun `imports bytes and computes correct sha256`() {
        val bytes = "hello world".toByteArray()
        val expectedHash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val dest = tempFolder.newFile()

        val result = importer.importToPrivateStorage(ByteArrayApkSource(bytes), dest)
        val fact = (result as AnalysisOutcome.Success).value

        assertEquals(expectedHash, fact.sha256)
        assertEquals(bytes.size.toLong(), fact.fileSizeBytes)
        assertEquals(bytes.toList(), dest.readBytes().toList())
    }

    @Test
    fun `rejects declared size over the hard limit before copying`() {
        val source =
            object : ApkSource {
                override val displayName = "huge.apk"
                override val declaredSizeBytes = AnalysisLimits.MAX_APK_FILE_SIZE_BYTES + 1
                override fun openStream(): InputStream = error("must not be opened")
            }
        val dest = tempFolder.newFile()
        val result = importer.importToPrivateStorage(source, dest)
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.FileTooLarge)
    }

    @Test
    fun `stops and deletes partial file when actual bytes exceed the limit with unknown declared size`() {
        val overLimit = AnalysisLimits.MAX_APK_FILE_SIZE_BYTES + 1024
        val source =
            object : ApkSource {
                override val displayName = "huge.apk"
                override val declaredSizeBytes: Long? = null

                override fun openStream(): InputStream =
                    object : InputStream() {
                        var produced = 0L

                        override fun read(): Int = error("unused")

                        override fun read(b: ByteArray, off: Int, len: Int): Int {
                            if (produced >= overLimit) return -1
                            val n = minOf(len.toLong(), overLimit - produced).toInt()
                            produced += n
                            return n
                        }
                    }
            }
        val dest = tempFolder.newFile()
        val result = importer.importToPrivateStorage(source, dest)
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.FileTooLarge)
        assertFalse(dest.exists())
    }

    @Test
    fun `cancellation deletes partial file and returns Cancelled`() {
        val bytes = ByteArray(1024 * 1024) { 1 }
        var reads = 0
        val dest = tempFolder.newFile()
        val result =
            importer.importToPrivateStorage(
                ByteArrayApkSource(bytes),
                dest,
                isActive = { reads++ < 2 },
            )
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.Cancelled)
        assertFalse(dest.exists())
    }

    @Test
    fun `read failure maps to UnreadableSource and deletes partial file`() {
        val source =
            object : ApkSource {
                override val displayName = "broken.apk"
                override val declaredSizeBytes: Long? = null

                override fun openStream(): InputStream =
                    object : InputStream() {
                        override fun read(): Int = throw java.io.IOException("boom")
                    }
            }
        val dest = tempFolder.newFile()
        val result = importer.importToPrivateStorage(source, dest)
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.UnreadableSource)
        assertFalse(dest.exists())
    }

    @Test
    fun `empty source is rejected as unsupported`() {
        val dest = tempFolder.newFile()
        val result = importer.importToPrivateStorage(ByteArrayApkSource(ByteArray(0)), dest)
        assertTrue((result as AnalysisOutcome.Failure).error is AnalysisError.UnsupportedFile)
    }

    @Test
    fun `incremental copy matches whole-file hash for larger input`() {
        val bytes = ByteArray(5 * 1024 * 1024) { (it % 251).toByte() }
        val dest = tempFolder.newFile()
        val result = importer.importToPrivateStorage(ByteArrayApkSource(bytes), dest, isActive = { true })
        val fact = (result as AnalysisOutcome.Success).value
        val expected = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals(expected, fact.sha256)
    }
}
