package com.demo.xray.engine.rules

import com.demo.xray.core.model.AnalysisFacts
import com.demo.xray.core.model.ApplicationFlags
import com.demo.xray.core.model.ArchiveEntryCategory
import com.demo.xray.core.model.ArchiveEntryFact
import com.demo.xray.core.model.ArchiveInventoryFact
import com.demo.xray.core.model.ComponentFact
import com.demo.xray.core.model.ComponentType
import com.demo.xray.core.model.ExportedState
import com.demo.xray.core.model.ImportedApkFact
import com.demo.xray.core.model.IntentFilterFact
import com.demo.xray.core.model.ManifestFacts
import com.demo.xray.core.model.PermissionFact
import com.demo.xray.core.model.PermissionRiskCategory
import com.demo.xray.core.model.UsesSdkFact

internal fun baseApplicationFlags() =
    ApplicationFlags(
        label = "Test App",
        debuggable = false,
        testOnly = false,
        allowBackup = false,
        usesCleartextTraffic = false,
        extractNativeLibs = null,
        requestLegacyExternalStorage = null,
        hasNetworkSecurityConfig = false,
        largeHeap = false,
        hasCode = true,
    )

internal fun baseManifest(
    permissions: List<PermissionFact> = emptyList(),
    components: List<ComponentFact> = emptyList(),
    applicationFlags: ApplicationFlags = baseApplicationFlags(),
    targetSdkVersion: Int? = 34,
    sharedUserId: String? = null,
) = ManifestFacts(
    packageName = "com.example.app",
    versionName = "1.0",
    versionCode = 1,
    compileSdkVersion = 34,
    usesSdk = UsesSdkFact(minSdkVersion = 26, targetSdkVersion = targetSdkVersion, maxSdkVersion = null),
    sharedUserId = sharedUserId,
    permissions = permissions,
    applicationFlags = applicationFlags,
    components = components,
)

internal fun baseArchive(entries: List<ArchiveEntryFact> = emptyList()) =
    ArchiveInventoryFact(
        entries = entries,
        totalCompressedSize = entries.sumOf { it.compressedSize },
        totalUncompressedSize = entries.sumOf { it.uncompressedSize },
    )

internal fun baseFacts(
    manifest: ManifestFacts = baseManifest(),
    archive: ArchiveInventoryFact = baseArchive(),
    targetSdkPolicyThreshold: Int = 33,
) = AnalysisFacts(
    imported = ImportedApkFact(sha256 = "0".repeat(64), fileSizeBytes = 1024, sourceDisplayName = "test.apk"),
    manifest = manifest,
    archive = archive,
    targetSdkPolicyThreshold = targetSdkPolicyThreshold,
)

internal fun permission(name: String, category: PermissionRiskCategory = PermissionRiskCategory.UNKNOWN) =
    PermissionFact(
        name = name,
        isCustom = false,
        protectionLevel = null,
        maxSdkVersion = null,
        requiredFeature = null,
        requiredNotFeature = null,
        usesPermissionFlags = null,
        riskCategory = category,
        friendlyName = null,
        group = null,
        explanation = null,
    )

internal fun component(
    name: String,
    type: ComponentType,
    effectiveExported: ExportedState = ExportedState.NOT_EXPORTED,
    permission: String? = null,
    readPermission: String? = null,
    writePermission: String? = null,
    intentFilters: List<IntentFilterFact> = emptyList(),
    isLauncher: Boolean = false,
) = ComponentFact(
    name = name,
    type = type,
    enabled = true,
    explicitExported = null,
    effectiveExported = effectiveExported,
    exportedExplanation = "test",
    permission = permission,
    processName = null,
    directBootAware = false,
    intentFilters = intentFilters,
    readPermission = readPermission,
    writePermission = writePermission,
    isLauncher = isLauncher,
)

internal fun archiveEntry(
    path: String,
    uncompressedSize: Long = 100,
    compressedSize: Long = 50,
    category: ArchiveEntryCategory = ArchiveEntryCategory.OTHER,
) = ArchiveEntryFact(
    path = path,
    isDirectory = false,
    compressedSize = compressedSize,
    uncompressedSize = uncompressedSize,
    category = category,
)
