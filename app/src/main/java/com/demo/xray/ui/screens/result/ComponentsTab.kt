package com.demo.xray.ui.screens.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.demo.xray.core.model.AnalysisResult
import com.demo.xray.core.model.ComponentFact
import com.demo.xray.core.model.ExportedState

private fun ExportedState.label(): String =
    when (this) {
        ExportedState.EXPORTED -> "Exported"
        ExportedState.NOT_EXPORTED -> "Not exported"
        ExportedState.UNKNOWN -> "Unknown"
    }

@Composable
internal fun ComponentsTab(result: AnalysisResult, modifier: Modifier) {
    val components = result.facts.manifest.components

    if (components.isEmpty()) {
        Text("No activities, services, receivers, or providers declared.", modifier = modifier.padding(16.dp))
        return
    }

    LazyColumn(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        items(components) { component -> ComponentRow(component) }
    }
}

@Composable
private fun ComponentRow(component: ComponentFact) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(component.name, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text(component.type.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.padding(top = 6.dp)) {
                AssistChip(onClick = {}, label = { Text(component.effectiveExported.label()) })
                if (!component.enabled) {
                    AssistChip(onClick = {}, label = { Text("Disabled") }, modifier = Modifier.padding(start = 6.dp))
                }
                if (component.intentFilters.isNotEmpty()) {
                    AssistChip(onClick = {}, label = { Text("${component.intentFilters.size} intent-filter(s)") }, modifier = Modifier.padding(start = 6.dp))
                }
            }
            Text(component.exportedExplanation, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            component.permission?.let { Text("Guarded by: $it", style = MaterialTheme.typography.bodySmall) }
            if (component.authorities.isNotEmpty()) {
                Text("Authorities: ${component.authorities.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
