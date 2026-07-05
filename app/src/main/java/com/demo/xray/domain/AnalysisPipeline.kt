package com.demo.xray.domain

import com.demo.xray.core.common.AnalysisLimits
import com.demo.xray.core.common.Clock
import com.demo.xray.core.common.DispatcherProvider
import com.demo.xray.core.model.AnalysisError
import com.demo.xray.core.model.AnalysisFacts
import com.demo.xray.core.model.AnalysisJobState
import com.demo.xray.core.model.AnalysisOutcome
import com.demo.xray.core.model.AnalysisProgress
import com.demo.xray.core.model.AnalysisResult
import com.demo.xray.core.model.PackageManagerCrossCheckFact
import com.demo.xray.core.model.map
import com.demo.xray.engine.apk.ANDROID_MANIFEST_ENTRY_NAME
import com.demo.xray.engine.apk.ApkImporter
import com.demo.xray.engine.apk.ApkSource
import com.demo.xray.engine.apk.ApkValidator
import com.demo.xray.engine.apk.ImportProgressListener
import com.demo.xray.engine.apk.axml.AxmlParser
import com.demo.xray.engine.apk.manifest.ManifestAnalyzer
import com.demo.xray.engine.apk.readEntryBounded
import com.demo.xray.engine.rules.FindingsEngine
import com.demo.xray.engine.rules.RULE_SET_VERSION
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

const val ENGINE_VERSION = "0.1.0-milestone1"

/**
 * Orchestrates the complete end-to-end APK analysis workflow.
 *
 * **Pipeline stages** (per PRD 13.4):
 *  1. **Import** (FR-003): Stream APK from content:// URI to private storage with incremental SHA-256
 *  2. **Validate** (FR-005): ZIP structure validation, archive inventory (categorize DEX/native/resources)
 *  3. **Manifest decode** (FR-030, FR-032): Parse binary AndroidManifest.xml via AXML parser
 *  4. **Cross-check** (FR-022, optional): Query PackageManager for label/icon (if installed)
 *  5. **Evaluate rules** (FR-110): Run all rules against facts, collect findings
 *  6. **Cleanup** (FR-140): Delete the imported APK copy (not retained by default)
 *
 * **Deferred to later phases**:
 *  - Signing inspection (would use apksig library)
 *  - DEX analysis (would use dexlib2)
 *  - SDK detection (would use fingerprint database)
 *  See IMPLEMENTATION_STATUS.md for full deferral list.
 *
 * **Design**:
 *  - Runs in a coroutine with three dispatcher types:
 *    * IO: import (disk/network I/O) and ZIP reading
 *    * Default: CPU-bound parsing and rule evaluation
 *  - Progress callbacks allow the UI to show live updates
 *  - Cooperative cancellation: can be cancelled by the caller (e.g., user dismisses the screen)
 *  - Typed error handling: all errors returned as AnalysisOutcome.Failure, never thrown
 */
class AnalysisPipeline(
    private val importer: ApkImporter,
    private val validator: ApkValidator,
    private val findingsEngine: FindingsEngine,
    private val dispatcherProvider: DispatcherProvider,
    private val clock: Clock,
) {
    /**
     * Run the complete analysis pipeline on an APK.
     *
     * @param source Where the APK comes from (e.g., content:// URI)
     * @param privateStorageDir Where to store the APK copy during import (usually app cache dir)
     * @param crossCheck Optional callback to query PackageManager (if the APK is installed)
     * @param onProgress Callback fired after each pipeline stage with progress information
     * @return Success(AnalysisResult) if analysis completed, Failure(error) on any error
     */
    suspend fun run(
        source: ApkSource,
        privateStorageDir: File,
        crossCheck: ((File) -> PackageManagerCrossCheckFact)? = null,
        onProgress: (AnalysisProgress) -> Unit = {},
    ): AnalysisOutcome<AnalysisResult> {
        val startElapsed = clock.elapsedRealtimeMs()
        val destFile = File(privateStorageDir, "${com.demo.xray.XRayApplication.TEMP_IMPORT_PREFIX}${UUID.randomUUID()}.apk")

        /**
         * Helper to fire a progress update with elapsed time.
         *
         * Invoked after each pipeline stage to notify the UI of progress
         * (used to update the progress bar, stage description, etc.).
         */
        fun progress(stage: AnalysisJobState, percent: Int?, description: String) {
            onProgress(AnalysisProgress(stage, percent, description, clock.elapsedRealtimeMs() - startElapsed))
        }

        /**
         * Capture the caller's coroutine Job for cooperative cancellation.
         *
         * The importer checks isActive() to abort early if the caller cancels the coroutine
         * (e.g., user dismisses the progress screen).
         *
         * We capture this before entering withContext() because suspension functions
         * can't access currentCoroutineContext() from within their body.
         */
        val callerJob = currentCoroutineContext()[Job]
        try {
            // ========== STAGE 1: IMPORT ==========
            // Copy the APK from its source (content:// URI) to app-private storage.
            // This is I/O-bound, so we run it on the IO dispatcher.
            progress(AnalysisJobState.IMPORTING, 0, "Copying selected file to private storage")
            val importResult =
                withContext(dispatcherProvider.io) {
                    importer.importToPrivateStorage(
                        source = source,
                        destination = destFile,
                        // isActive callback: abort import if caller cancels the coroutine (cooperative cancellation).
                        isActive = { callerJob?.isActive ?: true },
                        // Progress callback: update UI with copy progress (bytes copied).
                        onProgress =
                            ImportProgressListener { copied, total ->
                                val percent = total?.let { if (it > 0) ((copied * 100) / it).toInt() else null }
                                progress(AnalysisJobState.IMPORTING, percent, "Copied $copied bytes")
                            },
                    )
                }
            // Check for import failure and return early if one occurred.
            val importedFact = (importResult as? AnalysisOutcome.Success)?.value
                ?: return importResult as AnalysisOutcome.Failure

            // ========== STAGE 2: VALIDATE ==========
            // Check ZIP structure, enforce safety limits (file count, sizes), build archive inventory.
            progress(AnalysisJobState.VALIDATING, null, "Validating archive structure")
            val validateResult = withContext(dispatcherProvider.io) { validator.validate(destFile) }
            val archiveInventory = (validateResult as? AnalysisOutcome.Success)?.value
                ?: return validateResult as AnalysisOutcome.Failure

            // ========== STAGE 3: MANIFEST DECODE ==========
            // Extract and parse the binary AndroidManifest.xml (AXML format).
            progress(AnalysisJobState.ANALYZING_MANIFEST, null, "Decoding AndroidManifest.xml")
            val manifestFactsResult =
                withContext(dispatcherProvider.io) {
                    ZipFile(destFile).use { zip ->
                        // Find the manifest entry in the ZIP.
                        val entry = zip.getEntry(ANDROID_MANIFEST_ENTRY_NAME)
                            ?: return@use AnalysisOutcome.Failure(AnalysisError.MissingManifest())
                        // Read with size limit to prevent memory exhaustion.
                        readEntryBounded(zip, entry, AnalysisLimits.MAX_MANIFEST_ENTRY_BYTES).map { bytes ->
                            AxmlParser.parse(bytes)
                        }
                    }
                }
            // readEntryBounded and AxmlParser.parse both return AnalysisOutcome; flatten the nested result.
            // This is necessary because readEntryBounded can fail (RESOURCE_LIMIT_EXCEEDED) and
            // AxmlParser.parse can fail (MALFORMED_MANIFEST).
            val manifestFacts =
                when (manifestFactsResult) {
                    is AnalysisOutcome.Failure -> return manifestFactsResult
                    is AnalysisOutcome.Success ->
                        when (val axmlOutcome = manifestFactsResult.value) {
                            is AnalysisOutcome.Failure -> return axmlOutcome
                            is AnalysisOutcome.Success -> ManifestAnalyzer.analyze(axmlOutcome.value)
                        }
                }

            // ========== STAGE 4: CROSS-CHECK (optional) ==========
            // Query PackageManager for app label and icon (if the APK is installed on device).
            // Swallows errors (e.g., if PackageManager throws) to avoid failing the entire analysis.
            progress(AnalysisJobState.CROSS_CHECKING_PACKAGE_MANAGER, null, "Cross-checking package metadata")
            val crossCheckFact = crossCheck?.let { runCatching { it(destFile) }.getOrNull() }

            // Assemble all facts (manifest, archive inventory, cross-check result).
            val facts =
                AnalysisFacts(
                    imported = importedFact,
                    manifest = manifestFacts,
                    archive = archiveInventory,
                    targetSdkPolicyThreshold = AnalysisLimits.DEFAULT_TARGET_SDK_POLICY_THRESHOLD,
                    packageManagerCrossCheck = crossCheckFact,
                )

            // ========== STAGE 5: EVALUATE RULES ==========
            // Run all 24+ rules against the facts. CPU-bound, so we use the default dispatcher.
            progress(AnalysisJobState.EVALUATING_RULES, null, "Evaluating findings")
            val findings = withContext(dispatcherProvider.default) { findingsEngine.evaluate(facts) }

            // ========== COMPLETE ==========
            // Assemble the final result and return.
            progress(AnalysisJobState.COMPLETED, 100, "Analysis complete")
            val hasWarnings = manifestFacts.warnings.isNotEmpty() || archiveInventory.warnings.isNotEmpty()
            return AnalysisOutcome.Success(
                AnalysisResult(
                    jobId = UUID.randomUUID().toString(),
                    facts = facts,
                    findings = findings,
                    engineVersion = ENGINE_VERSION,
                    ruleSetVersion = RULE_SET_VERSION,
                    analyzedAtEpochMs = clock.nowEpochMs(),
                    durationMs = clock.elapsedRealtimeMs() - startElapsed,
                    status = if (hasWarnings) AnalysisJobState.COMPLETED_WITH_WARNINGS else AnalysisJobState.COMPLETED,
                ),
            )
        } finally {
            // ========== CLEANUP ==========
            // Per PRD FR-140: delete the imported APK copy to avoid storing APK files on device.
            // Exceptions during deletion are silently swallowed (best-effort cleanup).
            runCatching { if (destFile.exists()) destFile.delete() }
        }
    }
}
