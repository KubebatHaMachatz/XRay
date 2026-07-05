package com.demo.xray.ui.screens.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.demo.xray.core.model.AnalysisResult

/**
 * Structured summary of decoded manifest facts. A full raw canonical XML tree viewer (PRD
 * FR-031) is deferred; see IMPLEMENTATION_STATUS.md.
 */
@Composable
internal fun ManifestTab(result: AnalysisResult, modifier: Modifier) {
    val manifest = result.facts.manifest
    val flags = manifest.applicationFlags
    val rows =
        buildList {
            add("package" to (manifest.packageName ?: "?"))
            add("versionName / versionCode" to "${manifest.versionName ?: "?"} / ${manifest.versionCode ?: "?"}")
            add("sharedUserId" to (manifest.sharedUserId ?: "(none)"))
            add("uses-sdk min/target/max" to "${manifest.usesSdk.minSdkVersion ?: "?"} / ${manifest.usesSdk.targetSdkVersion ?: "?"} / ${manifest.usesSdk.maxSdkVersion ?: "?"}")
            add("application.debuggable" to flags.debuggable.toString())
            add("application.testOnly" to flags.testOnly.toString())
            add("application.allowBackup" to (flags.allowBackup?.toString() ?: "(absent, defaults true)"))
            add("application.usesCleartextTraffic" to (flags.usesCleartextTraffic?.toString() ?: "(absent)"))
            add("application.extractNativeLibs" to (flags.extractNativeLibs?.toString() ?: "(absent)"))
            add("application.requestLegacyExternalStorage" to (flags.requestLegacyExternalStorage?.toString() ?: "(absent)"))
            add("application.networkSecurityConfig present" to flags.hasNetworkSecurityConfig.toString())
            add("uses-feature count" to manifest.usesFeatures.size.toString())
            add("queries: packages" to manifest.queriesPackages.joinToString().ifBlank { "(none)" })
            add("queries: intent actions" to manifest.queriesIntentActions.joinToString().ifBlank { "(none)" })
            add("queries: provider authorities" to manifest.queriesProviderAuthorities.joinToString().ifBlank { "(none)" })
            add("QUERY_ALL_PACKAGES requested" to manifest.queriesAllPackages.toString())
            add("uses-library" to manifest.usesLibraries.joinToString().ifBlank { "(none)" })
            add("custom permissions declared" to manifest.customPermissions.size.toString())
            add("instrumentation entries" to manifest.instrumentation.size.toString())
        }

    LazyColumn(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        if (manifest.warnings.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Warnings", style = MaterialTheme.typography.labelMedium)
                        manifest.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        items(rows) { (label, value) ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
