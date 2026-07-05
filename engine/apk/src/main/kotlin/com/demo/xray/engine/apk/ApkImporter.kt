package com.demo.xray.engine.apk

import com.demo.xray.core.common.AnalysisLimits
import com.demo.xray.core.common.HashCalculator
import com.demo.xray.core.model.AnalysisError
import com.demo.xray.core.model.AnalysisOutcome
import com.demo.xray.core.model.ImportedApkFact
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun interface ImportProgressListener {
    fun onProgress(bytesCopied: Long, declaredTotalBytes: Long?)
}

/**
 * Streams an [ApkSource] into app-private storage while incrementally hashing it. See PRD
 * FR-003/FR-004. Never buffers the whole file in memory.
 */
interface ApkImporter {
    fun importToPrivateStorage(
        source: ApkSource,
        destination: File,
        isActive: () -> Boolean = { true },
        onProgress: ImportProgressListener = ImportProgressListener { _, _ -> },
    ): AnalysisOutcome<ImportedApkFact>
}

class DefaultApkImporter(
    private val hashCalculator: HashCalculator,
    private val bufferSize: Int = 256 * 1024,
) : ApkImporter {
    override fun importToPrivateStorage(
        source: ApkSource,
        destination: File,
        isActive: () -> Boolean,
        onProgress: ImportProgressListener,
    ): AnalysisOutcome<ImportedApkFact> {
        source.declaredSizeBytes?.let { declared ->
            if (declared > AnalysisLimits.MAX_APK_FILE_SIZE_BYTES) {
                return AnalysisOutcome.Failure(
                    AnalysisError.FileTooLarge(AnalysisLimits.MAX_APK_FILE_SIZE_BYTES, declared),
                )
            }
        }

        val digest = hashCalculator.newSha256Accumulator()
        val buffer = ByteArray(bufferSize)
        var totalBytes = 0L

        try {
            source.openStream().use { input ->
                FileOutputStream(destination).use { output ->
                    while (true) {
                        if (!isActive()) {
                            deleteQuietly(destination)
                            return AnalysisOutcome.Failure(AnalysisError.Cancelled)
                        }

                        val read = input.read(buffer)
                        if (read == -1) break

                        totalBytes += read
                        if (totalBytes > AnalysisLimits.MAX_APK_FILE_SIZE_BYTES) {
                            deleteQuietly(destination)
                            return AnalysisOutcome.Failure(
                                AnalysisError.FileTooLarge(AnalysisLimits.MAX_APK_FILE_SIZE_BYTES, null),
                            )
                        }

                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                        onProgress.onProgress(totalBytes, source.declaredSizeBytes)
                    }
                }
            }
        } catch (io: IOException) {
            deleteQuietly(destination)
            return AnalysisOutcome.Failure(AnalysisError.UnreadableSource(io::class.simpleName))
        }

        if (!isActive()) {
            deleteQuietly(destination)
            return AnalysisOutcome.Failure(AnalysisError.Cancelled)
        }

        if (totalBytes == 0L) {
            deleteQuietly(destination)
            return AnalysisOutcome.Failure(AnalysisError.UnsupportedFile("empty source"))
        }

        return AnalysisOutcome.Success(
            ImportedApkFact(
                sha256 = digest.digestHex(),
                fileSizeBytes = totalBytes,
                sourceDisplayName = source.displayName,
            ),
        )
    }

    private fun deleteQuietly(file: File) {
        runCatching { if (file.exists()) file.delete() }
    }
}
