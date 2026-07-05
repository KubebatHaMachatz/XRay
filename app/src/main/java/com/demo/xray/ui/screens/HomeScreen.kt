package com.demo.xray.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Home screen: welcome, disclaimer, and call-to-action button.
 *
 * **Layout pattern**:
 *  - Scaffold: provides a structured container with built-in support for top/bottom bars
 *  - Column: vertical layout, centered both horizontally and vertically
 *  - Material3 components: Button, Text with semantic styles from the theme
 *
 * **Callback-based**:
 *  - Takes a single callback [onAnalyzeClick] instead of navigating directly
 *  - Keeps this screen independent and testable (can use a mock callback)
 *  - The ViewModel decides what happens next (launch SAF picker)
 *
 * **Why Compose**:
 *  - Declarative: screen state is defined by @Composable functions
 *  - Reactive: when AnalysisUiState changes, Compose recomposes and swaps screens
 *  - No manual view inflation or XML parsing
 *  - Material3 theming built-in (dynamic colors on Android 12+)
 *
 * See DISCLAIMER constant below for the required legal notice.
 */
@Composable
fun HomeScreen(onAnalyzeClick: () -> Unit) {
    Scaffold { padding ->
        // Center-aligned column with generous padding.
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App name and tagline.
            Text("APK X-Ray", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Inspect an APK entirely on this device. Nothing is uploaded, and no network access is used.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )

            // Call-to-action button. Invokes the callback when tapped.
            Button(onClick = onAnalyzeClick) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Text(" Analyze APK", modifier = Modifier.padding(start = 4.dp))
            }

            // Legal disclaimer (required for all security analysis tools).
            Text(
                text = DISCLAIMER,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
    }
}

/**
 * Legal disclaimer to be shown on the home screen.
 *
 * **Purpose**: Clarify that this tool is for informational purposes only and is not a substitute
 * for a professional security review. Automated findings are risk indicators, not definitive
 * proof of vulnerability or safety. This helps manage user expectations and reduces liability.
 */
const val DISCLAIMER =
    "APK X-Ray performs static inspection of selected APK files entirely on this device. " +
        "Findings are automated risk indicators based on package contents and configuration. " +
        "They do not prove that an app is malicious, vulnerable, safe, or compliant, and they " +
        "are not a substitute for a complete security review or runtime analysis."
