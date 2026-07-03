package com.demo.xray.ui.state

import com.demo.xray.core.model.AnalysisError
import com.demo.xray.core.model.AnalysisProgress
import com.demo.xray.core.model.AnalysisResult

/**
 * Sealed UI state for the analysis workflow.
 *
 * **Design**: Using a sealed interface (Kotlin 1.9+) instead of `when` expressions checking
 * nullable fields means:
 *  - Compile-time exhaustiveness checking: if you add a new state, all uses must be updated
 *  - No null-safety bugs (no `if (result != null)` scattered everywhere)
 *  - Clear, self-documenting state transitions
 *  - Easy for the UI layer to decide which screen to show (Home/Progress/Result/Error)
 *
 * **State machine**:
 *  ```
 *  Home
 *    ↓ (user selects APK)
 *  InProgress
 *    ├→ Success (analysis completed)
 *    └→ Error (analysis failed)
 *    ↓ (user taps "New Analysis" or "Home")
 *  Home (cycle continues)
 *  ```
 *
 * **Notes**:
 *  - The ViewModel holds the current state in a MutableStateFlow<AnalysisUiState>
 *  - Compose observes the StateFlow and recomposes when state changes
 *  - No explicit navigation framework needed; navigation is implicit in state change
 *  - Results survive configuration changes because they're held in the ViewModel
 *  - Results are NOT persisted to database (deferred; see IMPLEMENTATION_STATUS.md)
 */
sealed interface AnalysisUiState {
    /**
     * Home screen state.
     *
     * Displayed when:
     *  - App first launches
     *  - User dismisses a result and taps "Home" or "New Analysis"
     *  - Analysis is cancelled
     */
    data object Home : AnalysisUiState

    /**
     * Analysis in progress.
     *
     * Contains live progress information (stage, percent, description, elapsed time).
     * The UI displays a progress screen with a circular/linear progress indicator and
     * a "Cancel" button (if user changes their mind).
     *
     * Transitions to Success or Error when the pipeline completes.
     */
    data class InProgress(val progress: AnalysisProgress) : AnalysisUiState

    /**
     * Analysis completed successfully.
     *
     * Contains the full AnalysisResult: facts (manifest, archive inventory, cross-check),
     * findings (list of security issues discovered), and metadata (engine version, duration).
     *
     * The UI displays a tabbed result viewer (Overview, Findings, Manifest, Permissions, Components).
     */
    data class Success(val result: AnalysisResult) : AnalysisUiState

    /**
     * Analysis failed with an error.
     *
     * The error contains a machine-readable code (e.g., "INVALID_ZIP"), a user-friendly message,
     * and optional diagnostic detail for developers.
     *
     * The UI displays an error screen with the message and a copyable diagnostic section.
     */
    data class Error(val error: AnalysisError) : AnalysisUiState
}
