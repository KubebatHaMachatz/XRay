package com.demo.xray.core.model

/**
 * Typed failure model for the analysis pipeline. See PRD FR-013.
 *
 * Design philosophy:
 *  - Each error subclass represents a distinct failure mode and is assigned a machine-readable
 *    [code] (e.g., "INVALID_ZIP") for logging/debugging.
 *  - [userMessage] is concise, user-friendly, and never contains a stack trace. It is displayed
 *    directly in the error screen.
 *  - [diagnosticDetail] is an optional non-sensitive string (exception class name, offset, etc.)
 *    surfaced only in a copyable diagnostics section for the user to share with developers.
 *
 * By avoiding exceptions and using sealed classes instead, we gain:
 *  - Exhaustive when-expressions at call sites (compiler catches missing cases)
 *  - No stack unwinding overhead
 *  - Easy logging of structured error data
 */
sealed class AnalysisError(
    val code: String,
    val userMessage: String,
    val diagnosticDetail: String? = null,
) {
    class UnsupportedFile(detail: String? = null) :
        AnalysisError("UNSUPPORTED_FILE", "This file is not a supported APK.", detail)

    class FileTooLarge(limitBytes: Long, actualBytes: Long?) :
        AnalysisError(
            "FILE_TOO_LARGE",
            "This file exceeds the maximum supported size of ${limitBytes / (1024 * 1024)} MiB.",
            "limit=$limitBytes actual=$actualBytes",
        )

    class UnreadableSource(detail: String? = null) :
        AnalysisError("UNREADABLE_SOURCE", "The selected file could not be read.", detail)

    class InvalidZip(detail: String? = null) :
        AnalysisError("INVALID_ZIP", "This file is not a valid ZIP/APK archive.", detail)

    class MissingManifest :
        AnalysisError("MISSING_MANIFEST", "This APK does not contain an AndroidManifest.xml.")

    class MalformedManifest(detail: String? = null) :
        AnalysisError("MALFORMED_MANIFEST", "The APK's manifest could not be parsed.", detail)

    class TooManyEntries(limit: Int, actual: Int? = null) :
        AnalysisError(
            "TOO_MANY_ENTRIES",
            "This archive contains more than the supported number of entries ($limit).",
            "limit=$limit actual=$actual",
        )

    class ResourceLimitExceeded(limitName: String) :
        AnalysisError(
            "RESOURCE_LIMIT_EXCEEDED",
            "Analysis stopped because a safety limit ($limitName) was exceeded.",
            limitName,
        )

    class UnsupportedDexVersion(detail: String? = null) :
        AnalysisError("UNSUPPORTED_DEX_VERSION", "One or more DEX files use an unsupported format.", detail)

    class SignatureInspectionUnavailable :
        AnalysisError(
            "SIGNATURE_INSPECTION_UNAVAILABLE",
            "Certificate inspection could not be completed for this APK.",
        )

    class DatabaseFailure(detail: String? = null) :
        AnalysisError("DATABASE_FAILURE", "Results could not be saved.", detail)

    object Cancelled : AnalysisError("CANCELLED", "Analysis was cancelled.")

    class UnexpectedInternalError(exceptionClass: String? = null) :
        AnalysisError("UNEXPECTED_INTERNAL_ERROR", "An unexpected error occurred.", exceptionClass)
}

/**
 * Typed result wrapper used across the analysis pipeline instead of throwing exceptions.
 *
 * This is a lightweight Result/Either type that makes error handling explicit and composable.
 * Compared to exception-based error handling:
 *  - Errors are part of the type signature (caught at compile time, not runtime)
 *  - No performance overhead from exception creation/unwinding
 *  - Easy to chain multiple operations and handle failures gracefully
 *  - Simplifies testing (no need to use @Test(expected=...) or assertThrows)
 *
 * @param T the type of a successful result
 */
sealed class AnalysisOutcome<out T> {
    /** Successful result containing a value */
    data class Success<T>(val value: T) : AnalysisOutcome<T>()

    /** Failure result containing an error (Nothing means this can never succeed) */
    data class Failure(val error: AnalysisError) : AnalysisOutcome<Nothing>()
}

/**
 * Functor map operation: if the outcome is Success, apply a transformation to the value.
 * If it's Failure, pass the error through unchanged.
 *
 * This allows chaining operations without nested when-expressions:
 *  ```
 *  validator.validate(file)
 *      .map { archive -> extractManifest(archive) }
 *      .map { bytes -> parseManifest(bytes) }
 *  ```
 * If any step fails, the Failure propagates to the end.
 */
inline fun <T, R> AnalysisOutcome<T>.map(transform: (T) -> R): AnalysisOutcome<R> =
    when (this) {
        is AnalysisOutcome.Success -> AnalysisOutcome.Success(transform(value))
        is AnalysisOutcome.Failure -> this
    }
