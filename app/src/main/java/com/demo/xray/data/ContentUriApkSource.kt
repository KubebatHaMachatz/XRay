package com.demo.xray.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.demo.xray.engine.apk.ApkSource
import java.io.IOException
import java.io.InputStream

/**
 * [ApkSource] backed by a `content://` URI from the Storage Access Framework. See PRD FR-001.
 * Kept as the only Android-framework-aware implementation of [ApkSource]; the import pipeline
 * itself (`:engine:apk`) never sees a `Uri` or `Context`.
 */
class ContentUriApkSource(
    private val context: Context,
    private val uri: Uri,
) : ApkSource {
    override val displayName: String? by lazy { queryStringColumn(OpenableColumns.DISPLAY_NAME) }

    override val declaredSizeBytes: Long? by lazy { querySizeColumn() }

    override fun openStream(): InputStream =
        context.contentResolver.openInputStream(uri) ?: throw IOException("openInputStream returned null for the selected document")

    private fun queryStringColumn(column: String): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()

    private fun querySizeColumn(): Long? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
            }
        }.getOrNull()
}
