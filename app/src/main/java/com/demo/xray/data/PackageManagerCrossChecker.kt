package com.demo.xray.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.demo.xray.core.model.PackageManagerCrossCheckFact
import java.io.File

/**
 * Cross-checks manifest-parser output against Android's public `PackageManager` archive
 * inspection API. See PRD FR-022. Deliberately best-effort: any failure here is non-fatal and
 * surfaces as a discrepancy note rather than failing the whole analysis.
 *
 * Icon loading (FR-020) is deferred to a later milestone: by the time a caller might want to
 * display it, the private APK copy has already been deleted per FR-140's default retention
 * policy, so rendering it would require restructuring around a `Bitmap` snapshot rather than a
 * live `Drawable`. See IMPLEMENTATION_STATUS.md.
 */
class PackageManagerCrossChecker(private val context: Context) {
    fun check(apkFile: File): PackageManagerCrossCheckFact {
        val info = loadPackageInfo(apkFile) ?: return PackageManagerCrossCheckFact(applicationLabel = null, iconLoaded = false, discrepancies = listOf("PackageManager.getPackageArchiveInfo returned null."))

        val applicationInfo = info.applicationInfo
        if (applicationInfo == null) {
            return PackageManagerCrossCheckFact(applicationLabel = null, iconLoaded = false, discrepancies = listOf("No ApplicationInfo returned by PackageManager."))
        }

        // Required so loadLabel can resolve resources from the private temp file rather than an
        // installed package location. See PRD FR-022.
        applicationInfo.sourceDir = apkFile.absolutePath
        applicationInfo.publicSourceDir = apkFile.absolutePath

        val label = runCatching { applicationInfo.loadLabel(context.packageManager).toString() }.getOrNull()

        return PackageManagerCrossCheckFact(applicationLabel = label, iconLoaded = false)
    }

    @Suppress("DEPRECATION")
    private fun loadPackageInfo(apkFile: File): PackageInfo? =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(GET_FLAGS.toLong()),
                )
            } else {
                context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, GET_FLAGS)
            }
        }.getOrNull()

    private companion object {
        const val GET_FLAGS =
            PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_SIGNING_CERTIFICATES
    }
}
