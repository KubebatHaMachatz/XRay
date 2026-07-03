package com.demo.xray.ui.screens.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.demo.xray.core.model.AnalysisResult
import com.demo.xray.ui.theme.color
import com.demo.xray.ui.theme.label
import java.text.DateFormat
import java.util.Date

@Composable
internal fun OverviewTab(result: AnalysisResult, modifier: Modifier, onNewAnalysis: () -> Unit) {
    val manifest = result.facts.manifest
    val rows =
        buildList {
            add("Package" to (manifest.packageName ?: "Unknown"))
            add("Application label" to (result.facts.packageManagerCrossCheck?.applicationLabel ?: manifest.applicationFlags.label ?: "Unknown"))
            add("Version" to "${manifest.versionName ?: "?"} (${manifest.versionCode ?: "?"})")
            add("Min / target / compile SDK" to "${manifest.usesSdk.minSdkVersion ?: "?"} / ${manifest.usesSdk.targetSdkVersion ?: "?"} / ${manifest.compileSdkVersion ?: "?"}")
            add("File size" to "${result.facts.imported.fileSizeBytes} bytes")
            add("SHA-256" to result.facts.imported.sha256)
            add("Source file" to (result.facts.imported.sourceDisplayName ?: "Unknown"))
            add("Analyzed" to DateFormat.getDateTimeInstance().format(Date(result.analyzedAtEpochMs)))
            add("Duration" to "${result.durationMs} ms")
            add("Engine / rule-set version" to "${result.engineVersion} / ${result.ruleSetVersion}")
            add("Status" to result.status.name)
        }

    LazyColumn(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                com.demo.xray.core.model.Severity.entries.forEach { severity ->
                    val count = result.findings.count { it.severity == severity }
                    if (count > 0) {
                        Column {
                            Text("$count", style = MaterialTheme.typography.titleLarge, color = severity.color())
                            Text(severity.label(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        items(rows) { (label, value) ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = if (label == "SHA-256") FontFamily.Monospace else FontFamily.Default)
                }
            }
        }
        item {
            Button(onClick = onNewAnalysis, modifier = Modifier.padding(top = 16.dp)) {
                Text("Analyze another APK")
            }
        }
    }
}
