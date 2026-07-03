package com.demo.xray.engine.apk.manifest

import com.demo.xray.core.model.ApplicationFlags
import com.demo.xray.core.model.ComponentFact
import com.demo.xray.core.model.ComponentType
import com.demo.xray.core.model.CustomPermissionFact
import com.demo.xray.core.model.InstrumentationFact
import com.demo.xray.core.model.IntentFilterDataFact
import com.demo.xray.core.model.IntentFilterFact
import com.demo.xray.core.model.ManifestFacts
import com.demo.xray.core.model.PermissionFact
import com.demo.xray.core.model.PermissionRiskCategory
import com.demo.xray.core.model.UsesSdkFact
import com.demo.xray.engine.apk.axml.AxmlDocument
import com.demo.xray.engine.apk.axml.AxmlElement

private const val LAUNCHER_CATEGORY = "android.intent.category.LAUNCHER"
private const val QUERY_ALL_PACKAGES_PERMISSION = "android.permission.QUERY_ALL_PACKAGES"

/**
 * Maps a decoded [AxmlDocument] into the typed, Android-framework-free [ManifestFacts] model.
 * See PRD FR-032. Every accessor tolerates a missing/malformed sub-tree by falling back to an
 * empty or null value rather than throwing, since manifests from hostile or malformed APKs are
 * expected input, not exceptional input.
 */
object ManifestAnalyzer {
    fun analyze(document: AxmlDocument): ManifestFacts {
        val manifestEl = document.root
        val warnings = mutableListOf<String>()

        val usesSdkEl = manifestEl.childrenNamed("uses-sdk").firstOrNull()
        val usesSdk =
            UsesSdkFact(
                minSdkVersion = usesSdkEl?.attr("minSdkVersion")?.asInt,
                targetSdkVersion = usesSdkEl?.attr("targetSdkVersion")?.asInt,
                maxSdkVersion = usesSdkEl?.attr("maxSdkVersion")?.asInt,
            )
        val targetSdk = usesSdk.targetSdkVersion
        if (usesSdkEl == null) warnings += "No <uses-sdk> element found; min/target SDK are unknown."

        val permissions =
            (manifestEl.childrenNamed("uses-permission") + manifestEl.childrenNamed("uses-permission-sdk-23"))
                .mapNotNull { toPermissionFact(it) }

        val customPermissions =
            manifestEl.childrenNamed("permission").mapNotNull { el ->
                el.attr("name")?.asString?.let { name ->
                    CustomPermissionFact(
                        name = name,
                        protectionLevel = el.attr("protectionLevel")?.asString,
                        label = el.attr("label")?.asString,
                        description = el.attr("description")?.asString,
                    )
                }
            }

        val usesFeatures = manifestEl.childrenNamed("uses-feature").mapNotNull { it.attr("name")?.asString }

        val queriesEl = manifestEl.childrenNamed("queries").firstOrNull()
        val queriesPackages = queriesEl?.childrenNamed("package")?.mapNotNull { it.attr("name")?.asString }.orEmpty()
        val queriesIntentActions =
            queriesEl
                ?.childrenNamed("intent")
                ?.flatMap { it.childrenNamed("action").mapNotNull { a -> a.attr("name")?.asString } }
                .orEmpty()
        val queriesProviderAuthorities =
            queriesEl?.childrenNamed("provider")?.mapNotNull { it.attr("authorities")?.asString }.orEmpty()
        val queriesAllPackages = permissions.any { it.name == QUERY_ALL_PACKAGES_PERMISSION }

        val applicationEl = manifestEl.childrenNamed("application").firstOrNull()
        if (applicationEl == null) warnings += "No <application> element found."

        val applicationFlags =
            ApplicationFlags(
                label = applicationEl?.attr("label")?.asString,
                debuggable = applicationEl?.attr("debuggable")?.asBoolean ?: false,
                testOnly = applicationEl?.attr("testOnly")?.asBoolean ?: false,
                allowBackup = applicationEl?.attr("allowBackup")?.asBoolean,
                usesCleartextTraffic = applicationEl?.attr("usesCleartextTraffic")?.asBoolean,
                extractNativeLibs = applicationEl?.attr("extractNativeLibs")?.asBoolean,
                requestLegacyExternalStorage = applicationEl?.attr("requestLegacyExternalStorage")?.asBoolean,
                hasNetworkSecurityConfig = applicationEl?.attr("networkSecurityConfig") != null,
                largeHeap = applicationEl?.attr("largeHeap")?.asBoolean ?: false,
                hasCode = applicationEl?.attr("hasCode")?.asBoolean ?: true,
            )

        val components = mutableListOf<ComponentFact>()
        applicationEl?.let { app ->
            components += app.childrenNamed("activity").mapNotNull { toComponentFact(it, ComponentType.ACTIVITY, targetSdk) }
            components += app.childrenNamed("activity-alias").mapNotNull { toComponentFact(it, ComponentType.ACTIVITY_ALIAS, targetSdk) }
            components += app.childrenNamed("service").mapNotNull { toComponentFact(it, ComponentType.SERVICE, targetSdk) }
            components += app.childrenNamed("receiver").mapNotNull { toComponentFact(it, ComponentType.RECEIVER, targetSdk) }
            components += app.childrenNamed("provider").mapNotNull { toComponentFact(it, ComponentType.PROVIDER, targetSdk) }
        }

        val usesLibraries = applicationEl?.childrenNamed("uses-library")?.mapNotNull { it.attr("name")?.asString }.orEmpty()

        val instrumentation =
            manifestEl.childrenNamed("instrumentation").mapNotNull { el ->
                el.attr("name")?.asString?.let { name ->
                    InstrumentationFact(
                        name = name,
                        targetPackage = el.attr("targetPackage")?.asString,
                        functionalTest = el.attr("functionalTest")?.asBoolean,
                        handleProfiling = el.attr("handleProfiling")?.asBoolean,
                    )
                }
            }

        return ManifestFacts(
            packageName = manifestEl.attr("package", namespaceUri = null)?.asString,
            versionName = manifestEl.attr("versionName")?.asString,
            versionCode = manifestEl.attr("versionCode")?.asInt?.toLong(),
            compileSdkVersion = manifestEl.attr("compileSdkVersion", namespaceUri = null)?.asInt,
            usesSdk = usesSdk,
            sharedUserId = manifestEl.attr("sharedUserId")?.asString,
            permissions = permissions,
            customPermissions = customPermissions,
            usesFeatures = usesFeatures,
            queriesPackages = queriesPackages,
            queriesIntentActions = queriesIntentActions,
            queriesProviderAuthorities = queriesProviderAuthorities,
            queriesAllPackages = queriesAllPackages,
            applicationFlags = applicationFlags,
            components = components,
            usesLibraries = usesLibraries,
            instrumentation = instrumentation,
            warnings = warnings,
        )
    }

    private fun toPermissionFact(el: AxmlElement): PermissionFact? {
        val name = el.attr("name")?.asString ?: return null
        val known = PermissionKnowledgeBase.lookup(name)
        val isCustom = !name.startsWith("android.permission.") && !name.startsWith("com.android.")
        return PermissionFact(
            name = name,
            isCustom = isCustom,
            protectionLevel = null,
            maxSdkVersion = el.attr("maxSdkVersion")?.asInt,
            requiredFeature = el.attr("requiredFeature")?.asString,
            requiredNotFeature = el.attr("requiredNotFeature")?.asString,
            usesPermissionFlags = el.attr("usesPermissionFlags")?.asInt,
            riskCategory = known?.riskCategory ?: PermissionRiskCategory.UNKNOWN,
            friendlyName = known?.friendlyName,
            group = known?.group,
            explanation = known?.explanation,
        )
    }

    private fun toComponentFact(el: AxmlElement, type: ComponentType, targetSdk: Int?): ComponentFact? {
        val name = el.attr("name")?.asString ?: return null
        val explicitExported = el.attr("exported")?.asBoolean
        val intentFilters = el.childrenNamed("intent-filter").map { toIntentFilterFact(it) }
        val exportResult = EffectiveExportCalculator.compute(type, explicitExported, intentFilters.isNotEmpty(), targetSdk)
        val metaData =
            el.childrenNamed("meta-data").mapNotNull { m ->
                val key = m.attr("name")?.asString ?: return@mapNotNull null
                key to (m.attr("value")?.asString ?: m.attr("resource")?.asString ?: "")
            }.toMap()

        return ComponentFact(
            name = name,
            type = type,
            enabled = el.attr("enabled")?.asBoolean ?: true,
            explicitExported = explicitExported,
            effectiveExported = exportResult.state,
            exportedExplanation = exportResult.explanation,
            permission = el.attr("permission")?.asString,
            processName = el.attr("process")?.asString,
            directBootAware = el.attr("directBootAware")?.asBoolean ?: false,
            intentFilters = intentFilters,
            metaData = metaData,
            authorities =
                if (type == ComponentType.PROVIDER) {
                    el.attr("authorities")?.asString?.split(";")?.map { it.trim() }.orEmpty()
                } else {
                    emptyList()
                },
            readPermission = el.attr("readPermission")?.asString,
            writePermission = el.attr("writePermission")?.asString,
            grantUriPermissions = el.attr("grantUriPermissions")?.asBoolean ?: false,
            multiprocess = el.attr("multiprocess")?.asBoolean,
            initOrder = el.attr("initOrder")?.asInt,
            isLauncher = intentFilters.any { it.categories.contains(LAUNCHER_CATEGORY) },
            targetActivity = el.attr("targetActivity")?.asString,
        )
    }

    private fun toIntentFilterFact(el: AxmlElement): IntentFilterFact =
        IntentFilterFact(
            actions = el.childrenNamed("action").mapNotNull { it.attr("name")?.asString },
            categories = el.childrenNamed("category").mapNotNull { it.attr("name")?.asString },
            data =
                el.childrenNamed("data").map { d ->
                    IntentFilterDataFact(
                        scheme = d.attr("scheme")?.asString,
                        host = d.attr("host")?.asString,
                        port = d.attr("port")?.asString,
                        path = d.attr("path")?.asString,
                        pathPrefix = d.attr("pathPrefix")?.asString,
                        pathPattern = d.attr("pathPattern")?.asString,
                        mimeType = d.attr("mimeType")?.asString,
                    )
                },
            priority = el.attr("priority")?.asInt,
            autoVerify = el.attr("autoVerify")?.asBoolean ?: false,
        )
}
