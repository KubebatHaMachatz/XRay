package com.demo.xray.ui.screens.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demo.xray.core.model.AnalysisResult
import com.demo.xray.core.model.Severity
import com.demo.xray.ui.theme.color
import com.demo.xray.ui.theme.icon
import com.demo.xray.ui.theme.label

@Composable
internal fun FindingsTab(result: AnalysisResult, modifier: Modifier) {
    var activeFilter by remember { mutableStateOf<Severity?>(null) }
    val findings = result.findings.sortedByDescending { it.severity.ordinal }
    val visible = findings.filter { activeFilter == null || it.severity == activeFilter }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(modifier = Modifier.padding(8.dp)) {
            item {
                FilterChip(
                    selected = activeFilter == null,
                    onClick = { activeFilter = null },
                    label = { Text("All (${findings.size})") },
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            items(Severity.entries.reversed()) { severity ->
                val count = findings.count { it.severity == severity }
                if (count > 0) {
                    FilterChip(
                        selected = activeFilter == severity,
                        onClick = { activeFilter = if (activeFilter == severity) null else severity },
                        label = { Text("${severity.label()} ($count)") },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            Text(
                text = if (findings.isEmpty()) "No findings for this APK." else "No findings match the selected filter.",
                modifier = Modifier.padding(16.dp),
            )
        }

        LazyColumn {
            items(visible) { finding ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(finding.severity.icon(), contentDescription = finding.severity.label(), tint = finding.severity.color())
                            Text(finding.severity.label(), style = MaterialTheme.typography.labelMedium, color = finding.severity.color())
                            Text(finding.ruleId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(finding.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                        finding.affected?.let {
                            Text("Affected: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        finding.evidence.forEach { evidence ->
                            Text("• $evidence", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "Remediation: ${finding.remediation}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
