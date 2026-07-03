package com.demo.xray.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.demo.xray.AppContainer
import com.demo.xray.core.model.AnalysisJobState
import com.demo.xray.core.model.AnalysisOutcome
import com.demo.xray.core.model.AnalysisProgress
import com.demo.xray.data.ContentUriApkSource
import com.demo.xray.data.PackageManagerCrossChecker
import com.demo.xray.ui.state.AnalysisUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the analysis workflow and UI state.
 *
 * **Responsibilities**:
 *  - Hold the current UI state (Home / InProgress / Success / Error) in a reactive StateFlow
 *  - Trigger APK analysis when the user selects a file (onApkSelected)
 *  - Cancel in-flight analysis if the user selects a new file or dismisses the progress screen
 *  - Transform pipeline outcomes (Success/Failure) into UI states
 *
 * **Lifecycle**:
 *  - The ViewModel survives configuration changes (screen rotations) thanks to
 *    Android's ViewModel framework
 *  - The in-flight analysis coroutine (stored as [analysisJob]) runs in viewModelScope,
 *    which is cancelled when the ViewModel is cleared (when the hosting Activity/Fragment is destroyed)
 *  - Note: this is a simple coroutine-based job manager; a production app would use WorkManager
 *    for background jobs that need to survive process death (see IMPLEMENTATION_STATUS.md)
 *
 * **State management**: Unidirectional data flow:
 *  ```
 *  User selects APK → onApkSelected() → pipeline runs → progress updates → outcome → _state updated → UI recomposes
 *  ```
 */
class AnalysisViewModel(
    private val container: AppContainer,
    private val appContext: Context,
) : ViewModel() {
    /** Current UI state, exposed as a read-only StateFlow for Compose to observe. */
    private val _state = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Home)
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    /** The currently running analysis job (if any); used for cancellation. */
    private var analysisJob: Job? = null

    /**
     * User selected an APK file to analyze.
     *
     * Cancels any prior analysis and launches a new one in viewModelScope.
     * Updates the UI state with progress callbacks from the pipeline.
     *
     * @param uri Content URI of the selected APK (from SAF DocumentsProvider)
     */
    fun onApkSelected(uri: Uri) {
        // Cancel the prior analysis if the user selected a new file before the first one finished.
        analysisJob?.cancel()

        analysisJob =
            viewModelScope.launch {
                // Update UI to "in progress" immediately so the user sees the progress screen.
                _state.value = AnalysisUiState.InProgress(AnalysisProgress(AnalysisJobState.QUEUED, 0, "Starting analysis", 0))

                // Wrap the content:// URI in an ApkSource abstraction for the pipeline.
                val source = ContentUriApkSource(appContext, uri)

                // Create the cross-checker (queries PackageManager for app label and icon).
                val crossChecker = PackageManagerCrossChecker(appContext)

                // Run the full analysis pipeline: import → validate → manifest → rules.
                // Passes a progress callback so the pipeline can update the UI as it proceeds.
                val outcome =
                    container.analysisPipeline.run(
                        source = source,
                        privateStorageDir = appContext.cacheDir,
                        crossCheck = crossChecker::check,
                        onProgress = { progress -> _state.value = AnalysisUiState.InProgress(progress) },
                    )

                // Transform the outcome (Success/Failure) into a UI state for the UI to render.
                _state.value =
                    when (outcome) {
                        is AnalysisOutcome.Success -> AnalysisUiState.Success(outcome.value)
                        is AnalysisOutcome.Failure -> AnalysisUiState.Error(outcome.error)
                    }
            }
    }

    /**
     * User dismissed the progress screen or cancelled the analysis.
     *
     * Cancels the in-flight analysis job (if any) and returns to the home screen.
     */
    fun cancelAnalysis() {
        analysisJob?.cancel()
        _state.value = AnalysisUiState.Home
    }

    /**
     * User triggered a "New Analysis" action from the result screen.
     *
     * Resets to home state, clearing the previous result.
     */
    fun reset() {
        analysisJob?.cancel()
        _state.value = AnalysisUiState.Home
    }

    /**
     * Factory for ViewModel creation.
     *
     * Inject dependencies here instead of using the default factory because we need
     * the AppContainer and Context.
     */
    class Factory(private val container: AppContainer, private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AnalysisViewModel(container, appContext) as T
    }
}
