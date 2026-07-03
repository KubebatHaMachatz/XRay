package com.demo.xray.core.model

/** Analysis job states. See PRD FR-010. */
enum class AnalysisJobState {
    QUEUED,
    IMPORTING,
    VALIDATING,
    ANALYZING_MANIFEST,
    ANALYZING_ARCHIVE,
    ANALYZING_SIGNATURE,
    ANALYZING_DEX,
    DETECTING_SDKS,
    EVALUATING_RULES,
    PERSISTING_RESULTS,
    COMPLETED,
    COMPLETED_WITH_WARNINGS,
    CANCELLED,
    FAILED,
}

val AnalysisJobState.isTerminal: Boolean
    get() =
        this == AnalysisJobState.COMPLETED ||
            this == AnalysisJobState.COMPLETED_WITH_WARNINGS ||
            this == AnalysisJobState.CANCELLED ||
            this == AnalysisJobState.FAILED

data class AnalysisProgress(
    val stage: AnalysisJobState,
    val overallPercent: Int?,
    val stageDescription: String,
    val elapsedMs: Long,
)
