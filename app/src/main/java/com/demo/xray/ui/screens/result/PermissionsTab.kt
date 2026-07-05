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
import com.demo.xray.core.model.PermissionFact
import com.demo.xray.core.model.PermissionRiskCategory

private fun PermissionRiskCategory.label(): String =
    when (this) {
        PermissionRiskCategory.NORMAL -> "Normal"
        PermissionRiskCategory.SENSITIVE -> "Sensitive"
        PermissionRiskCategory.PRIVILEGED_SYSTEM -> "Privileged/system"
        PermissionRiskCategory.SPECIAL_ACCESS -> "Special access"
        PermissionRiskCategory.UNKNOWN -> "Unknown"
    }

@Composable
internal fun PermissionsTab(result: AnalysisResult, modifier: Modifier) {
    val permissions = result.facts.manifest.permissions.sortedBy { it.name }

    if (permissions.isEmpty()) {
        Text("No permissions requested.", modifier = modifier.padding(16.dp))
        return
    }

    LazyColumn(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        items(permissions) { permission -> PermissionRow(permission) }
    }
}

@Composable
private fun PermissionRow(permission: PermissionFact) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(permission.name, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            permission.friendlyName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            permission.explanation?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(modifier = Modifier.padding(top = 6.dp)) {
                AssistChip(onClick = {}, label = { Text(permission.riskCategory.label()) })
                if (permission.isCustom) {
                    AssistChip(onClick = {}, label = { Text("Custom") }, modifier = Modifier.padding(start = 6.dp))
                }
                permission.maxSdkVersion?.let {
                    AssistChip(onClick = {}, label = { Text("maxSdkVersion=$it") }, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
