package com.demo.xray.ui.screens.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.demo.xray.core.model.AnalysisResult

private enum class ResultTab(val label: String) {
    OVERVIEW("Overview"),
    FINDINGS("Findings"),
    MANIFEST("Manifest"),
    PERMISSIONS("Permissions"),
    COMPONENTS("Components"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(result: AnalysisResult, onNewAnalysis: () -> Unit) {
    var selectedTab by remember { mutableStateOf(ResultTab.OVERVIEW) }

    BackHandler(onBack = onNewAnalysis)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(result.facts.manifest.packageName ?: "Analysis result") },
                navigationIcon = {
                    IconButton(onClick = onNewAnalysis) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == ResultTab.OVERVIEW,
                    onClick = { selectedTab = ResultTab.OVERVIEW },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text(ResultTab.OVERVIEW.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == ResultTab.FINDINGS,
                    onClick = { selectedTab = ResultTab.FINDINGS },
                    icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                    label = { Text("Findings (${result.findings.size})") },
                )
                NavigationBarItem(
                    selected = selectedTab == ResultTab.MANIFEST,
                    onClick = { selectedTab = ResultTab.MANIFEST },
                    icon = { Icon(Icons.Filled.Description, contentDescription = null) },
                    label = { Text(ResultTab.MANIFEST.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == ResultTab.PERMISSIONS,
                    onClick = { selectedTab = ResultTab.PERMISSIONS },
                    icon = { Icon(Icons.Filled.VpnKey, contentDescription = null) },
                    label = { Text("Permissions (${result.facts.manifest.permissions.size})") },
                )
                NavigationBarItem(
                    selected = selectedTab == ResultTab.COMPONENTS,
                    onClick = { selectedTab = ResultTab.COMPONENTS },
                    icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                    label = { Text("Components (${result.facts.manifest.components.size})") },
                )
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (selectedTab) {
            ResultTab.OVERVIEW -> OverviewTab(result, content)
            ResultTab.FINDINGS -> FindingsTab(result, content)
            ResultTab.MANIFEST -> ManifestTab(result, content)
            ResultTab.PERMISSIONS -> PermissionsTab(result, content)
            ResultTab.COMPONENTS -> ComponentsTab(result, content)
        }
    }
}
