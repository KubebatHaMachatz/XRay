package com.demo.xray

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.demo.xray.ui.AnalysisViewModel
import com.demo.xray.ui.screens.ErrorScreen
import com.demo.xray.ui.screens.HomeScreen
import com.demo.xray.ui.screens.ProgressScreen
import com.demo.xray.ui.screens.result.ResultScreen
import com.demo.xray.ui.state.AnalysisUiState
import com.demo.xray.ui.theme.XRayTheme

/** MIME types accepted by the SAF picker. See PRD FR-001. */
private val APK_MIME_TYPES =
    arrayOf(
        "application/vnd.android.package-archive",
        "application/octet-stream",
    )

class MainActivity : ComponentActivity() {
    private val viewModel: AnalysisViewModel by viewModels {
        AnalysisViewModel.Factory((application as XRayApplication).container, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pickApkLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let { viewModel.onApkSelected(it) }
            }

        setContent {
            XRayTheme {
                val state by viewModel.state.collectAsState()
                when (val current = state) {
                    is AnalysisUiState.Home -> HomeScreen(onAnalyzeClick = { pickApkLauncher.launch(APK_MIME_TYPES) })
                    is AnalysisUiState.InProgress -> ProgressScreen(progress = current.progress, onCancel = viewModel::cancelAnalysis)
                    is AnalysisUiState.Success -> ResultScreen(result = current.result, onNewAnalysis = viewModel::reset)
                    is AnalysisUiState.Error -> ErrorScreen(error = current.error, onDismiss = viewModel::reset)
                }
            }
        }
    }
}
