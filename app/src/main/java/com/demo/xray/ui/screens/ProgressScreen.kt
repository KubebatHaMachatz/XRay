package com.demo.xray.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demo.xray.core.model.AnalysisProgress

private fun stageLabel(stage: com.demo.xray.core.model.AnalysisJobState): String =
    when (stage) {
        com.demo.xray.core.model.AnalysisJobState.QUEUED -> "Queued"
        com.demo.xray.core.model.AnalysisJobState.IMPORTING -> "Importing"
        com.demo.xray.core.model.AnalysisJobState.VALIDATING -> "Validating archive"
        com.demo.xray.core.model.AnalysisJobState.ANALYZING_MANIFEST -> "Decoding manifest"
        com.demo.xray.core.model.AnalysisJobState.ANALYZING_ARCHIVE -> "Analyzing archive"
        com.demo.xray.core.model.AnalysisJobState.ANALYZING_SIGNATURE -> "Inspecting signature"
        com.demo.xray.core.model.AnalysisJobState.ANALYZING_DEX -> "Analyzing DEX"
        com.demo.xray.core.model.AnalysisJobState.DETECTING_SDKS -> "Detecting SDKs"
        com.demo.xray.core.model.AnalysisJobState.CROSS_CHECKING_PACKAGE_MANAGER -> "Cross-checking package metadata"
        com.demo.xray.core.model.AnalysisJobState.EVALUATING_RULES -> "Evaluating findings"
        com.demo.xray.core.model.AnalysisJobState.PERSISTING_RESULTS -> "Saving results"
        com.demo.xray.core.model.AnalysisJobState.COMPLETED,
        com.demo.xray.core.model.AnalysisJobState.COMPLETED_WITH_WARNINGS -> "Complete"
        com.demo.xray.core.model.AnalysisJobState.CANCELLED -> "Cancelled"
        com.demo.xray.core.model.AnalysisJobState.FAILED -> "Failed"
    }

@Composable
fun ProgressScreen(progress: AnalysisProgress, onCancel: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stageLabel(progress.stage), style = MaterialTheme.typography.titleMedium)
            Text(progress.stageDescription, style = MaterialTheme.typography.bodySmall)

            val percent = progress.overallPercent
            if (percent != null) {
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
            }

            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}
