package com.demo.xray.engine.apk

import java.io.InputStream

/**
 * Abstraction over the byte source selected by the user. Kept free of Android framework types
 * (no `Uri`, no `ContentResolver`) so the import pipeline is unit-testable on the JVM; the `:app`
 * layer supplies an implementation backed by `ContentResolver.openInputStream`.
 */
interface ApkSource {
    /** Display name for the source, e.g. the document's file name. Never a full path. */
    val displayName: String?

    /** Declared length in bytes if the provider reliably exposes one, else null. */
    val declaredSizeBytes: Long?

    fun openStream(): InputStream
}

/** Simple [ApkSource] backed by an in-memory or already-materialized byte array. Test utility. */
class ByteArrayApkSource(
    private val bytes: ByteArray,
    override val displayName: String? = "test.apk",
    private val reportDeclaredSize: Boolean = true,
) : ApkSource {
    override val declaredSizeBytes: Long? = if (reportDeclaredSize) bytes.size.toLong() else null

    override fun openStream(): InputStream = bytes.inputStream()
}
