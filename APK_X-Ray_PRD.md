# APK X-Ray — Product Requirements Document

**Document type:** Product Requirements Document (PRD) and implementation specification  
**Product name:** APK X-Ray  
**Repository name:** `apk-xray`  
**Primary platform:** Android  
**Implementation language:** Kotlin  
**Document version:** 1.0  
**Status:** Ready for implementation  
**Intended implementer:** AI coding agent supervised by a human developer

---

## 1. Executive summary

APK X-Ray is a privacy-preserving Android application that analyzes Android APK files entirely on the user's device. The user selects an APK through Android's system file picker, and the app inspects its package metadata, merged manifest, permissions, application components, signing certificates, native libraries, DEX contents, embedded SDK indicators, archive composition, and potential security or compatibility concerns.

The application must not upload APKs or analysis results, must not require a dedicated server, and must not require Internet access. APK X-Ray is an inspection and engineering-assistance tool, not an antivirus product, malware detector, vulnerability scanner, app installer, or substitute for a professional security audit.

The project is intended to be a polished open-source portfolio application. Its codebase must demonstrate production-grade Android engineering: modular architecture, unidirectional data flow, offline-first local persistence, safe handling of untrusted binary input, structured concurrency, accessibility, adaptive UI, automated testing, performance measurement, CI, documentation, and reproducible reports.

---

## 2. Problem statement

Developers, security reviewers, QA engineers, technical support teams, and advanced Android users often need to understand what is inside an APK without installing it or transferring it to a third-party service. Existing desktop tools are useful but are not always available on a mobile device, and many web-based APK analyzers require users to upload proprietary or sensitive binaries.

APK X-Ray addresses this need by providing a transparent, local-only APK inspection workflow on Android.

---

## 3. Product goals

### 3.1 Primary goals

1. Allow a user to select an APK and receive a useful analysis without installing or executing it.
2. Perform all processing locally on the device.
3. Present raw facts separately from heuristic findings.
4. Detect common manifest, signing, packaging, privacy, compatibility, and size concerns.
5. Compare two APK versions and clearly show additions, removals, and changes.
6. Export reproducible human-readable and machine-readable reports.
7. Remain responsive and stable when processing malformed, unusually large, or intentionally hostile APK files.
8. Provide a codebase suitable for discussion in senior Android engineering interviews.
9. Maintain a comprehensive automated test suite and enforce quality gates in CI.

### 3.2 Portfolio goals

The repository should visibly demonstrate:

- Modern Kotlin and Jetpack Compose.
- Clean boundaries between UI, domain, persistence, and binary-analysis code.
- Safe parsing of untrusted files.
- Coroutines, Flow, cancellation, and bounded concurrency.
- Room persistence and schema migration discipline.
- Test-driven development for pure logic and parsers.
- Unit, integration, instrumented UI, migration, and performance tests.
- Baseline Profiles and Macrobenchmark usage.
- Accessibility and adaptive layouts.
- CI, static analysis, dependency verification, and release documentation.
- Architecture Decision Records explaining non-obvious choices.

---

## 4. Non-goals

The MVP must not:

1. Install, launch, execute, dynamically load, or sandbox the analyzed APK.
2. Use `DexClassLoader`, reflection over APK classes, native loading, or any mechanism that executes analyzed code.
3. Claim that an APK is safe, malicious, exploitable, or free of vulnerabilities.
4. Decompile DEX bytecode into Java or Kotlin source code.
5. Modify, resign, patch, optimize, or rebuild APKs.
6. Analyze runtime network traffic or dynamic behavior.
7. upload APKs, hashes, package names, reports, telemetry, or crash data.
8. Enumerate all installed applications on the device.
9. Depend on a backend, account system, subscription, API key, or cloud database.
10. Analyze Android App Bundles (`.aab`), split APK sets (`.apks`), XAPK packages, or installed split packages in the MVP.
11. Retrieve Play Store metadata.
12. provide legal, compliance, or definitive security certification.

---

## 5. Target users

### 5.1 Primary personas

**Android developer**  
Needs to inspect a release APK, verify the final merged manifest, identify packaged SDKs, understand size composition, and compare builds.

**Mobile security reviewer**  
Needs a quick first-pass review of exported components, risky permissions, signing information, cleartext settings, and other risk indicators without uploading a client APK.

**QA or release engineer**  
Needs to compare a previous release with a candidate build and verify that permissions, components, SDKs, ABIs, or signing identity did not unexpectedly change.

**Technical support engineer**  
Needs a shareable report containing package, version, SDK, architecture, and certificate data.

### 5.2 Secondary persona

**Advanced Android user**  
Wants to inspect an APK obtained outside the Play Store before deciding whether to install it, while understanding that the app does not certify safety.

---

## 6. Product principles

1. **Local by design:** The release APK must not request the `INTERNET` permission.
2. **Facts before conclusions:** Raw evidence must always be available behind every finding.
3. **No false certainty:** Findings are risk indicators, not proof of a vulnerability or malicious behavior.
4. **Unknown is a valid result:** Missing or unparseable data must produce an explicit `Unknown` or `Not evaluated` state rather than a guessed value.
5. **Untrusted input:** Every byte in an imported APK must be treated as potentially malformed or hostile.
6. **Do not execute:** APK content is data only.
7. **Bounded work:** File size, entry count, decompressed bytes, memory use, concurrency, and processing time must be bounded.
8. **Reproducibility:** Reports must include the APK SHA-256 hash, analysis-engine version, rule-set version, and timestamp.
9. **Accessibility:** All core workflows must be usable with TalkBack, large fonts, keyboard navigation, and high-contrast system settings.
10. **Testability:** Platform-independent logic must remain outside Android framework classes whenever practical.

---

## 7. Success metrics

The project is successful when:

1. At least 95% of valid fixture APKs complete analysis without a crash.
2. All malformed fixture APKs fail gracefully with a typed, user-readable error.
3. The app never executes code from an analyzed APK.
4. The release manifest contains no `INTERNET` permission.
5. A 100 MB reference APK completes analysis in 15 seconds or less on the defined reference device.
6. A 500 MB reference APK completes analysis in 60 seconds or less on the defined reference device, subject to DEX complexity.
7. The UI remains responsive and cancellable throughout analysis.
8. The rule engine has at least 90% line coverage and 85% branch coverage.
9. Overall non-generated production code has at least 80% line coverage.
10. All critical user journeys pass instrumented tests on the minimum supported API and latest stable API in CI or scheduled CI.
11. Reports from identical APK bytes and identical engine/rule versions are structurally deterministic except for report generation time and analysis duration.

### 7.1 Reference performance device

Use one documented physical or virtual reference configuration for repeatable measurements. The repository must record:

- Device or emulator model.
- Android/API version.
- CPU architecture.
- Available RAM.
- Build type.
- Thermal state where measurable.

Performance numbers are acceptance targets for that reference configuration, not universal guarantees.

---

## 8. Supported platform and toolchain

- **Minimum Android version:** API 26.
- **Compile SDK:** Latest stable SDK available when implementation begins.
- **Target SDK:** Latest stable SDK available when implementation begins.
- **Language:** Kotlin.
- **UI:** Jetpack Compose with Material 3.
- **Build system:** Gradle Kotlin DSL.
- **Dependency catalog:** `gradle/libs.versions.toml`.
- **Java toolchain:** Latest stable version supported by the selected Android Gradle Plugin; pin it in the repository.
- **Dependency policy:** Stable releases only unless an Architecture Decision Record explicitly justifies a preview dependency.

Exact dependency versions must be pinned and committed. Do not use dynamic versions such as `+`, version ranges, or unpinned Git branches.

---

## 9. MVP scope

### 9.1 Included in MVP

1. APK selection through the system picker.
2. Secure local import and validation.
3. Package and build metadata.
4. Decoded merged manifest viewer.
5. Permissions analysis.
6. Activities, activity aliases, services, receivers, and providers analysis.
7. Intent-filter inspection.
8. Signing certificate inspection and signature-verification status where technically reliable.
9. APK archive and size breakdown.
10. DEX inventory and class-package scanning.
11. Embedded SDK detection using a bundled fingerprint database.
12. Native library and ABI analysis.
13. Rule-based findings with evidence and remediation.
14. Analysis history containing results but not APK bytes by default.
15. Comparison of two analyzed APKs.
16. Markdown, HTML, and JSON report export.
17. Share report through the Android Sharesheet.
18. Search, filtering, sorting, dark mode, and adaptive layouts.
19. Unit, integration, instrumentation, migration, and benchmark tests.
20. GitHub Actions CI and project documentation.

### 9.2 Post-MVP candidates

- AAB and APK-set analysis.
- Static secret-pattern scanning.
- Software Bill of Materials generation.
- Certificate trust-list comparison.
- User-defined SDK fingerprints.
- User-defined rules using a safe declarative format.
- Diff of decoded resources.
- Local YARA-like string rules without code execution.
- Optional analysis of an APK explicitly shared into APK X-Ray.
- Desktop companion or Kotlin Multiplatform parser library.

---

## 10. Core user journeys

### 10.1 Analyze an APK

1. User opens APK X-Ray.
2. Home screen explains that analysis is local and does not certify safety.
3. User taps **Analyze APK**.
4. System document picker opens.
5. User selects a file.
6. App validates and copies the selected content to private temporary storage while calculating SHA-256.
7. App creates an analysis job and shows staged progress.
8. User may leave the screen; analysis continues using a lifecycle-resilient background mechanism.
9. User may cancel the job.
10. On completion, the app opens the analysis summary.
11. User navigates among findings, manifest, permissions, components, SDKs, signing, size, native libraries, DEX, and files.
12. User may export or share a report.

### 10.2 Compare two APKs

1. User opens **Compare**.
2. User selects two completed analyses from history or imports missing APKs.
3. App warns when package names differ but allows comparison.
4. App calculates a deterministic structured diff.
5. User views summary deltas and detailed added/removed/changed items.
6. User exports a comparison report.

### 10.3 Review analysis history

1. User opens **History**.
2. Analyses are grouped by package name and sorted by analysis time.
3. Each item displays icon, label, package, version, hash prefix, date, and finding counts.
4. User can search, filter, open, compare, rename, or delete an analysis.
5. Deleting an analysis removes all associated result records and any retained temporary file.

---

## 11. Functional requirements

## 11.1 File selection and import

### FR-001 — Select APK

The app must use Android's Storage Access Framework and an Activity Result contract to select a document. Prefer `OpenDocument` because it works with document providers and permits temporary or persistent URI access. Accepted MIME types should include:

- `application/vnd.android.package-archive`
- `application/octet-stream`
- A controlled `*/*` fallback when a provider does not expose the APK MIME type

The app must not rely on filename extension alone.

### FR-002 — Receive shared APK

The app may support `ACTION_SEND` for a single content URI in the MVP if it does not delay core delivery. The same validation and import pipeline must be used. Do not duplicate parsing logic.

### FR-003 — Import into private storage

The selected content must be streamed into an app-private temporary file before random-access analysis.

During the copy, the app must:

- Calculate SHA-256 incrementally.
- Count bytes and enforce the hard input-size limit.
- Support cancellation.
- Avoid loading the entire file into memory.
- Delete partial files after failure or cancellation.
- Display copy progress when the provider exposes a reliable size.
- Display indeterminate progress when size is unknown.

### FR-004 — Input limits

MVP hard limits:

- Maximum APK file size: 1 GiB.
- Maximum ZIP entries: 100,000.
- Maximum Android manifest entry size read into memory: 16 MiB.
- Maximum single metadata/text entry read into memory: 32 MiB unless a specialized streaming parser is used.
- Maximum aggregate bytes intentionally decompressed by non-DEX analyzers: 2 GiB.
- Maximum reported ZIP expansion ratio before a warning: 1,000:1.

Limits must be constants in a documented policy object and covered by tests. Exceeding a limit must return a typed error, not a crash.

### FR-005 — Validate APK structure

Before full analysis, validate:

- ZIP magic and readable central directory.
- Presence of `AndroidManifest.xml`.
- No duplicate critical entry names that create ambiguous interpretation.
- Entry names do not contain path traversal sequences or invalid normalization.
- Declared sizes and offsets are plausible.

The app must not extract the archive tree to the filesystem.

### FR-006 — URI permissions

Retain URI permission only until import is safely complete unless the user explicitly enables **Keep source link for re-analysis**. Results history must work without access to the original source URI.

---

## 11.2 Analysis job and progress

### FR-010 — Persistent analysis job

Each import creates an `AnalysisJob` with a stable UUID and these states:

- `QUEUED`
- `IMPORTING`
- `VALIDATING`
- `ANALYZING_MANIFEST`
- `ANALYZING_ARCHIVE`
- `ANALYZING_SIGNATURE`
- `ANALYZING_DEX`
- `DETECTING_SDKS`
- `EVALUATING_RULES`
- `PERSISTING_RESULTS`
- `COMPLETED`
- `COMPLETED_WITH_WARNINGS`
- `CANCELLED`
- `FAILED`

A lifecycle-resilient mechanism such as WorkManager must own long-running analysis. The UI observes job state from Room/Flow rather than owning the analysis coroutine directly.

### FR-011 — Progress model

Progress must include:

- Current stage.
- Overall percentage when calculable.
- Stage-specific progress.
- Elapsed time.
- Current non-sensitive operation description.
- Cancel action.

Progress weighting must be deterministic and documented. It may be adjusted using measured stage durations, but tests must not depend on wall-clock timing.

### FR-012 — Cancellation

Cancellation must be cooperative and checked:

- During import copy.
- Between ZIP entries.
- Between DEX files.
- During SDK fingerprint matching.
- Before database transactions.
- During report generation.

After cancellation, partial results may be discarded. If retained for debugging, they must not appear as completed analyses.

### FR-013 — Failure handling

Failures must use a typed error model, including at minimum:

- `UnsupportedFile`
- `FileTooLarge`
- `UnreadableSource`
- `InvalidZip`
- `MissingManifest`
- `MalformedManifest`
- `TooManyEntries`
- `ResourceLimitExceeded`
- `UnsupportedDexVersion`
- `SignatureInspectionUnavailable`
- `DatabaseFailure`
- `Cancelled`
- `UnexpectedInternalError`

User messages must be concise and must not expose stack traces. A copyable diagnostic section may include a non-sensitive error code, engine version, stage, and exception class.

---

## 11.3 General package metadata

### FR-020 — Package summary

Display:

- App icon when safely loadable.
- Application label when resolvable.
- Package/application ID.
- Version name.
- Version code as a 64-bit value.
- Minimum SDK.
- Target SDK.
- Compile SDK when present in the final manifest metadata.
- APK byte size.
- SHA-256 hash.
- File name and source display name.
- Analysis date and duration.
- Analysis-engine version.
- Rule-set version.
- Number of DEX files.
- Detected ABIs.
- Signing status summary.
- Counts of permissions, components, SDK detections, native libraries, and findings.

### FR-021 — Metadata confidence

Every field derived from incomplete or heuristic parsing must expose its source and confidence. Examples:

- `Manifest`
- `PackageManager archive inspection`
- `ZIP inventory`
- `DEX scan`
- `Heuristic`

### FR-022 — PackageManager cross-check

Use `PackageManager.getPackageArchiveInfo()` with public flags as a high-level cross-check for package metadata, components, permissions, and signing certificates. It must not be the only manifest parser because Android does not provide archive intent filters through this API.

Before loading an archive icon or label, set the returned `ApplicationInfo.sourceDir` and `publicSourceDir` to the app-private temporary APK path. Treat failure to render the icon or label as non-fatal.

---

## 11.4 Manifest inspection

### FR-030 — Binary Android XML parser

The app must decode the binary `AndroidManifest.xml` from the APK into a typed internal model. It must preserve:

- Element names.
- Attribute namespaces.
- Attribute names.
- Raw typed values.
- Resolved human-readable values where possible.
- Resource references when unresolved.
- Element order where practical.

Use a maintained, license-compatible AXML parser or implement a focused parser with comprehensive golden and malformed-input tests. Do not use hidden Android APIs. Record the dependency and license decision in an ADR.

### FR-031 — Canonical manifest viewer

Provide a searchable, read-only XML-like view with:

- Syntax differentiation.
- Line wrapping toggle.
- Copy selected value.
- Copy full decoded manifest.
- Search next/previous.
- Expand/collapse sections if rendered as a tree.
- Raw and normalized attribute values.

The displayed manifest must be escaped safely and must never be rendered as executable HTML.

### FR-032 — Parsed manifest facts

Parse at minimum:

- `<uses-sdk>`
- `<uses-permission>` and SDK-specific permission variants
- `<permission>` and custom permission protection level
- `<uses-feature>`
- `<queries>`
- `<application>` attributes
- `<activity>`
- `<activity-alias>`
- `<service>`
- `<receiver>`
- `<provider>`
- `<intent-filter>` actions, categories, and data declarations
- `<meta-data>`
- `<uses-library>`
- `<instrumentation>`

### FR-033 — Effective exported state

For each component, store and display:

- Explicit `android:exported` value: `true`, `false`, or absent.
- Computed effective exported state according to component type, target SDK, intent filters, and relevant Android rules.
- Explanation of how the value was computed.
- `Unknown` when the app cannot reliably compute it.

The effective-export computation must be an isolated, pure, table-driven function with exhaustive tests across component types and target SDK boundaries.

---

## 11.5 Permission analysis

### FR-040 — Requested permissions

Display all requested permissions with:

- Full permission name.
- Friendly name when known.
- Android permission group when known.
- Protection level when known.
- Whether the permission is platform-defined or custom.
- SDK qualifiers such as `maxSdkVersion`.
- A concise local explanation.
- Risk category: normal, sensitive, privileged/system, special access, unknown.

Do not label every dangerous permission as a vulnerability.

### FR-041 — Custom permissions

Display custom permission declarations with:

- Name.
- Protection level.
- Label and description references.
- Components guarded by the permission.
- Whether the permission appears weaker than expected for an exported component.

### FR-042 — Permission filtering

Support filters for:

- Sensitive only.
- Custom only.
- Platform only.
- Special access.
- Unknown.
- Added/removed in comparison mode.

### FR-043 — Permission knowledge base

The permission explanation database must be bundled with the application, versioned, and usable offline. Unknown future permissions must still display correctly by full name.

---

## 11.6 Component analysis

### FR-050 — Component list

List activities, aliases, services, receivers, and providers. Each row must include:

- Fully qualified component name.
- Enabled state.
- Explicit and effective exported state.
- Required permission.
- Process name.
- Direct-boot awareness.
- Intent-filter count.
- Finding count.

### FR-051 — Component detail

Show all relevant manifest attributes and intent-filter contents. For providers also show:

- Authorities.
- Read permission.
- Write permission.
- URI permission grants.
- Multiprocess flag when present.
- Initialization order when present.

### FR-052 — Search and filters

Support search by class name, action, category, authority, and permission. Filters must include exported, not exported, unknown export state, enabled, disabled, and has findings.

---

## 11.7 Signing and certificate inspection

### FR-060 — Certificate information

Display signer information when available:

- Number of current signers.
- Signing-certificate lineage when available.
- Subject distinguished name.
- Issuer distinguished name.
- Serial number.
- Valid-from and valid-until dates.
- Public-key algorithm and size when derivable.
- Signature algorithm.
- SHA-256 certificate fingerprint.
- SHA-1 fingerprint for compatibility/reference, clearly marked as legacy.
- Whether the APK uses multiple signers.

### FR-061 — Signature schemes

The desired result is to report the presence and verification result of APK Signature Schemes v1, v2, v3, and v4 where technically possible on-device.

Implementation requirements:

1. Use public `PackageManager` archive APIs for certificate inspection.
2. Conduct a time-boxed compatibility spike for a vetted Android-compatible implementation of AOSP `apksig`.
3. Pin the selected library version and document its license and compatibility.
4. Do not call AOSP `apksig` internal packages.
5. If reliable full verification is unavailable on supported devices, the MVP must report **Certificate inspected; full scheme verification unavailable** rather than implying successful verification.
6. Never derive a `Verified` status solely from the presence of certificate bytes.

### FR-062 — Signing findings

Generate findings for:

- Signature verification failed.
- No signer information could be obtained.
- Signer changed between compared APKs.
- Multiple current signers.
- Certificate currently outside its validity interval, with an explanation that certificate dates alone do not necessarily determine Android installability.
- Signature lineage changed unexpectedly.

Signer change between versions must be prominent, but a valid signing-certificate rotation lineage must be distinguished from an unrelated signer.

---

## 11.8 Archive and size analysis

### FR-070 — Archive inventory

Build a logical archive tree without extracting files. For every retained entry store:

- Normalized path.
- Directory flag.
- Compression method.
- Compressed size.
- Uncompressed size when known.
- CRC when available.
- Category.
- Suspicion flags such as duplicate name, invalid path, extreme expansion ratio, or unknown size.

### FR-071 — Categories

Aggregate archive size by at least:

- DEX.
- Native libraries.
- Resources.
- Compiled resources and manifest.
- Assets.
- `META-INF` and signing metadata.
- Kotlin metadata.
- Other.

### FR-072 — Size visualizations

Provide:

- Category percentage chart.
- Compressed versus uncompressed totals.
- Top 20 largest entries.
- Folder tree sorted by size or name.
- Compression-ratio display.
- Per-ABI native size.

Charts must have text alternatives and must not be the only way to understand the data.

### FR-073 — Suspicious archive conditions

Report but do not necessarily reject:

- Very high compression ratio.
- Large uncompressed assets.
- Duplicate non-critical entries.
- Unknown entry sizes.
- Unusually high file count.

Reject only when configured hard safety limits are exceeded or structure is ambiguous for critical entries.

---

## 11.9 DEX analysis

### FR-080 — DEX inventory

Detect `classes.dex`, `classes2.dex`, and additional numbered DEX files. For each DEX file display:

- Compressed and uncompressed size.
- DEX format/version.
- Class count.
- Method-reference count when available.
- Field-reference count when available.
- String count when available.
- Parsing status and warnings.

### FR-081 — Class descriptor scanning

Use Google's maintained smali/dexlib2 artifacts, or an equivalently maintained permissive parser, to enumerate class descriptors without decompiling method bodies unless required for a future feature.

MVP scanning should read only the minimum needed for:

- Package prefix inventory.
- SDK fingerprint matching.
- Class counts.
- High-level DEX metrics.

### FR-082 — DEX safety

- Process one DEX file at a time unless profiling proves bounded parallel processing is safe.
- Avoid materializing all class names as duplicate strings when a streaming or sequence-based approach is possible.
- Check cancellation regularly.
- Treat unsupported DEX versions as partial-analysis warnings.
- Never load DEX classes into the runtime.

---

## 11.10 Embedded SDK detection

### FR-090 — SDK fingerprint database

Bundle a versioned JSON database in app assets. Each fingerprint record must support:

- Stable SDK ID.
- Display name.
- Vendor.
- Category such as advertising, analytics, attribution, crash reporting, social, payments, security, or utility.
- One or more class/package prefixes.
- Optional exact class names.
- Optional manifest metadata keys.
- Optional component names.
- Optional native-library patterns.
- Optional archive-file patterns.
- Positive and negative indicators.
- Confidence weights.
- Source note and last-reviewed date.

### FR-091 — Detection result

Each detected SDK must show:

- SDK name and vendor.
- Category.
- Confidence: high, medium, or low.
- Matched evidence.
- DEX files or manifest locations that supplied evidence.
- Fingerprint database version.

Do not infer an exact SDK version unless an explicit, reliable marker is present.

### FR-092 — Detection scoring

Detection scoring must be deterministic and tested. Suggested policy:

- Exact distinctive class: high weight.
- Multiple package-prefix matches: medium weight.
- Generic package prefix alone: low weight.
- Manifest metadata plus package evidence: confidence boost.
- Negative indicator: subtract or suppress.

The UI must not present low-confidence results as certain.

### FR-093 — Fingerprint maintenance

The repository must include:

- JSON schema.
- Validation test.
- Duplicate-ID test.
- Prefix-collision test.
- Documentation for adding a fingerprint.
- License/source attribution where needed.

No remote update mechanism is required in the MVP.

---

## 11.11 Native library analysis

### FR-100 — ABI inventory

Identify native libraries under `lib/<abi>/`. Support known Android ABIs and preserve unknown ABI directory names.

Display:

- ABI list.
- Library names per ABI.
- Compressed and uncompressed sizes.
- Whether the APK is 32-bit only, 64-bit only, or mixed.
- Libraries missing from one ABI compared with another.

### FR-101 — ELF inspection

Basic ELF-header inspection may be implemented to report architecture and bitness. Full symbol or vulnerability analysis is out of scope for the MVP.

Malformed ELF files must produce a warning and must not terminate the complete analysis.

---

## 11.12 Rule-based findings engine

### FR-110 — Rule contract

Each rule must implement a stable interface similar to:

```kotlin
interface AnalysisRule {
    val id: String
    val metadata: RuleMetadata
    fun evaluate(facts: AnalysisFacts): RuleResult
}
```

A rule result must be one of:

- `Pass`
- `Finding`
- `NotApplicable`
- `Unknown`

A finding must contain:

- Stable rule ID.
- Title.
- Category.
- Severity: `INFO`, `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`.
- Confidence: `LOW`, `MEDIUM`, or `HIGH`.
- Explanation.
- Evidence list.
- Affected manifest path, component, permission, entry, signer, or SDK when applicable.
- Suggested remediation.
- Rule-set version.

### FR-111 — No aggregate security score

The MVP must not calculate a single safety, trust, malware, or security score. The summary may show counts by severity and category.

### FR-112 — Initial rule set

Implement and test at least the following rules. Severity values below are defaults and may be adjusted through review, but rule IDs must remain stable.

| Rule ID | Default severity | Condition |
|---|---:|---|
| `SIGNATURE_VERIFICATION_FAILED` | Critical | Full cryptographic verification was attempted and failed. |
| `SIGNATURE_STATUS_UNKNOWN` | Info | Full verification was unavailable or inconclusive. |
| `APP_DEBUGGABLE` | High | Final manifest explicitly enables `android:debuggable`. |
| `APP_TEST_ONLY` | Medium | Final manifest enables `android:testOnly`. |
| `CLEARTEXT_TRAFFIC_ALLOWED` | Medium | Manifest explicitly permits cleartext traffic. |
| `EXPORTED_PROVIDER_UNPROTECTED` | High | Effective exported provider lacks appropriate read/write/general permission. |
| `EXPORTED_SERVICE_UNPROTECTED` | High | Effective exported service lacks a guarding permission. |
| `EXPORTED_RECEIVER_UNPROTECTED` | Medium | Effective exported receiver lacks a guarding permission; expected public broadcasts must be explained as potential exceptions. |
| `EXPORTED_ACTIVITY_REVIEW` | Medium | Effective exported non-launcher activity has no permission; deep-link cases must be shown as review items, not definite vulnerabilities. |
| `WEAK_CUSTOM_PERMISSION_GUARD` | Medium | Exported component is guarded only by a custom permission with weak protection. |
| `SHARED_USER_ID_DECLARED` | Medium | Deprecated shared-user identity is declared. |
| `BACKUP_CONFIGURATION_REVIEW` | Low | Backup is enabled or ambiguous and sensitive-data implications require review. |
| `LEGACY_EXTERNAL_STORAGE` | Low | Legacy external-storage behavior is requested. |
| `QUERY_ALL_PACKAGES_REQUESTED` | Medium | Broad package visibility is requested. |
| `REQUEST_INSTALL_PACKAGES` | High | APK requests package installation capability. |
| `SYSTEM_ALERT_WINDOW_REQUESTED` | High | APK requests overlay capability. |
| `ACCESSIBILITY_SERVICE_DECLARED` | High | An accessibility service is declared; this is a high-impact capability requiring review, not proof of abuse. |
| `BACKGROUND_LOCATION_REQUESTED` | Medium | Background location is requested. |
| `SMS_OR_CALL_LOG_PERMISSION` | High | SMS, call-log, or related high-impact permission is requested. |
| `SENSITIVE_PERMISSION_COMBINATION` | Medium | Configured combination such as camera + microphone + background location is present. |
| `TARGET_SDK_BELOW_POLICY` | Medium | Target SDK is below the bundled policy threshold. |
| `ONLY_32_BIT_NATIVE_LIBS` | Low | Native code exists but only 32-bit ABIs are present. |
| `NATIVE_LIBRARY_ABI_MISMATCH` | Low | Native libraries are inconsistent across included ABIs. |
| `LARGE_UNCOMPRESSED_ENTRY` | Low | A single uncompressed entry exceeds the policy threshold. |
| `EXTREME_ZIP_EXPANSION_RATIO` | High | Archive metadata indicates an extreme expansion ratio. |
| `DUPLICATE_CRITICAL_ENTRY` | High | Duplicate manifest or DEX critical entry creates ambiguity. |
| `SIGNER_CHANGED_IN_COMPARISON` | Critical | Compared APKs with the same package have unrelated current signing identities. |
| `NEW_SENSITIVE_PERMISSION` | High | Comparison reveals a newly requested sensitive permission. |
| `NEW_EXPORTED_COMPONENT` | High | Comparison reveals a newly effective exported component. |
| `NEW_TRACKING_SDK` | Medium | Comparison reveals a newly detected advertising, attribution, or analytics SDK. |

### FR-113 — Rule evidence

Every finding must show exactly which facts triggered it. Rules must not emit a finding when required facts are unavailable; they must return `Unknown`.

### FR-114 — Rule documentation

Generate or maintain a `RULES.md` file containing:

- Rule ID.
- Rationale.
- Logic summary.
- Expected false positives.
- Severity rationale.
- Suggested remediation.
- Test cases.

---

## 11.13 Analysis results UI

### FR-120 — Summary screen

The summary must include:

- App identity and version.
- Hash prefix with copy action.
- Analysis completeness.
- Signing summary.
- Findings grouped by severity.
- Counts of permissions, exported components, SDKs, ABIs, DEX files, and archive size.
- Primary actions: export, compare, delete.

### FR-121 — Navigation

On compact screens, use bottom navigation or top-level tabs appropriate to the number of destinations. On larger screens, use an adaptive navigation rail or drawer and a list-detail layout where useful.

Recommended result destinations:

- Overview.
- Findings.
- Manifest.
- Permissions.
- Components.
- SDKs.
- Signing.
- Size and files.
- DEX and native.

### FR-122 — Search

Global search should find:

- Permission names.
- Component names.
- Intent actions/categories.
- Provider authorities.
- SDK names.
- Archive paths.
- Finding titles and rule IDs.

If global search is too large for the first MVP iteration, every detailed section must at least provide local search.

### FR-123 — Empty and partial states

Each section must distinguish:

- No items present.
- Analysis not performed.
- Analysis unsupported.
- Analysis failed for this section.
- Results truncated due to a safety limit.

---

## 11.14 Comparison

### FR-130 — Comparison eligibility

Allow comparison of any two completed analyses. When package names differ, display a persistent warning and disable assumptions that require package identity.

### FR-131 — Diff categories

Compare at minimum:

- Package and version metadata.
- File size and category sizes.
- SHA-256.
- Min/target/compile SDK.
- Requested and custom permissions.
- Components and effective exported state.
- Intent filters.
- Manifest application flags.
- Signers and certificate lineage.
- SDK detections.
- Native ABIs and libraries.
- DEX counts and metrics.
- Findings.
- Archive entries.

### FR-132 — Diff representation

Every diff item must be classified as:

- Added.
- Removed.
- Changed.
- Unchanged, hidden by default.
- Unknown due to incomplete analysis.

Stable semantic keys must be used. Do not compare display order or database IDs.

### FR-133 — Important changes

Highlight:

- Signing identity changes.
- New sensitive permissions.
- New exported components.
- Target SDK regression.
- New high-impact capabilities.
- Newly detected tracking SDKs.
- Large size regressions.
- Removed 64-bit ABI support.

### FR-134 — Size thresholds

Default significant size change:

- Absolute increase greater than 5 MiB, or
- Relative increase greater than 10%.

Both thresholds must be configurable in code policy and reported in comparison metadata.

---

## 11.15 History and local persistence

### FR-140 — Store results, not APKs

By default, persist analysis results and metadata but delete the imported APK temporary file after analysis and report generation are complete.

A user setting may permit retaining the private APK copy for re-analysis. It must be opt-in, show storage usage, and permit deletion.

### FR-141 — History operations

Users must be able to:

- Open.
- Search.
- Rename the local analysis label.
- Compare.
- Export again.
- Delete one analysis.
- Delete all analyses.

### FR-142 — Storage management

Settings must show:

- Result database size.
- Retained APK cache size.
- Temporary file size.
- Clear temporary files.
- Delete all data.

Startup must remove abandoned temporary files older than a documented threshold when they are not referenced by an active job.

---

## 11.16 Reports

### FR-150 — Report formats

Support:

1. Markdown human-readable report.
2. Self-contained HTML human-readable report with no remote assets or JavaScript.
3. Versioned JSON machine-readable report.

### FR-151 — Report contents

Reports must include:

- Product and engine version.
- Rule-set and SDK-fingerprint versions.
- Report schema version.
- Analysis timestamp and duration.
- Source display name.
- APK SHA-256.
- Package metadata.
- Analysis completeness and warnings.
- Findings with evidence and remediation.
- Permissions.
- Components and intent filters.
- Signing data.
- SDK detections and confidence.
- Native and DEX data.
- Size summary and largest entries.
- Disclaimer.

The full archive-entry list may be optional in human reports but must be configurable in JSON export.

### FR-152 — Report safety

- Escape all APK-controlled text in HTML.
- Use no external fonts, scripts, images, or network references.
- Sanitize suggested filenames.
- Do not embed APK bytes.
- Do not include source URI unless the user explicitly opts in.
- Avoid including device identifiers.

### FR-153 — Export and share

Use `ACTION_CREATE_DOCUMENT` or the equivalent Activity Result contract for saving. Use a `FileProvider` and Android Sharesheet for sharing temporary reports. Grant URI permissions narrowly and revoke or expire temporary files.

### FR-154 — Deterministic JSON schema

The JSON schema must be committed under `docs/schema/`. Fields must have stable names, documented nullability, and an explicit schema version. Breaking changes require a schema-version increment.

---

## 11.17 Settings and about

### FR-160 — Settings

Include:

- System/light/dark theme.
- Dynamic color on/off.
- Keep APK copies after analysis: off by default.
- Include full archive inventory in JSON exports.
- Default report format.
- Large-entry warning threshold.
- Size regression thresholds.
- Clear history and cache.

Security-rule severities and logic are not user-configurable in the MVP.

### FR-161 — About and transparency

Display:

- App version.
- Open-source license.
- Third-party licenses.
- Analysis-engine version.
- Rule-set version.
- SDK-fingerprint version.
- Privacy statement: all analysis is local.
- Clear disclaimer that findings are not a malware verdict or complete security audit.

---

## 12. UX and visual design

### 12.1 Visual direction

Use a technical but approachable visual style:

- Material 3.
- Clear hierarchy and dense information without clutter.
- Monospace type only for hashes, package names, manifest values, and file paths.
- Severity icons plus labels; never rely on color alone.
- Expandable evidence cards.
- Sticky search/filter controls on long lists.
- Skeleton or stage-specific loading rather than indefinite blank screens.

### 12.2 Accessibility requirements

- Minimum touch targets consistent with Android guidance.
- Content descriptions for non-text controls.
- Charts have semantic summaries.
- Findings expose severity as text.
- Support font scaling to at least 200% without clipped core actions.
- Logical TalkBack focus order.
- Keyboard/D-pad support for main workflows.
- Do not encode added/removed states with red/green alone.
- Automated Compose semantics tests for critical screens.

### 12.3 Adaptive behavior

- Compact: single-pane navigation.
- Medium: navigation rail and wider lists.
- Expanded: list-detail where suitable, especially findings, components, and files.
- Preserve selected item and scroll/search state across window changes.

---

## 13. Architecture

## 13.1 Architectural style

Use:

- Layered architecture.
- Unidirectional data flow.
- Immutable UI state.
- Repository boundaries.
- Coroutines and Flow.
- Constructor injection through Hilt or a similarly testable DI solution.
- Pure domain models that do not depend on Compose or Android framework types.

Android framework objects such as `Uri`, `PackageInfo`, and `Context` must not leak into domain or report schemas.

## 13.2 Recommended modules

```text
:app
:core:model
:core:common
:core:designsystem
:core:database
:core:files
:core:testing
:engine:apk
:engine:rules
:engine:report
:feature:home
:feature:analysis
:feature:history
:feature:compare
:feature:settings
:benchmark
:fixtures
```

The implementer may merge very small modules, but the binary-analysis engine, rule engine, report generator, database, and UI must remain cleanly separated.

## 13.3 Key interfaces

At minimum define abstractions equivalent to:

```kotlin
interface ApkImporter
interface ApkValidator
interface ManifestAnalyzer
interface ArchiveAnalyzer
interface DexAnalyzer
interface SignatureAnalyzer
interface NativeLibraryAnalyzer
interface SdkDetector
interface FindingsEngine
interface AnalysisRepository
interface ComparisonEngine
interface ReportGenerator
interface TemporaryFileStore
interface Clock
interface HashCalculator
```

Each interface must have production and test implementations where useful.

## 13.4 Analysis pipeline

Recommended pipeline:

1. Resolve source metadata.
2. Stream to private temporary file and calculate SHA-256.
3. Validate input and ZIP policy.
4. Inventory archive entries.
5. Parse manifest into typed facts.
6. Cross-check public `PackageManager` archive metadata.
7. Inspect signing certificates and verification status.
8. Analyze DEX files and class/package descriptors.
9. Detect SDKs.
10. Analyze native libraries and ABIs.
11. Evaluate rules.
12. Persist one completed snapshot transactionally.
13. Delete temporary APK unless retention is enabled.

Stages may execute concurrently only after the private file is complete and only with bounded concurrency. Default maximum analyzer concurrency should be two until profiling proves a higher value safe.

## 13.5 Consistency and transactions

- Job state updates may be persisted incrementally.
- A completed analysis snapshot must become visible atomically.
- Findings must reference the exact fact snapshot and rule-set version used.
- Comparison operates only on completed immutable snapshots.
- Deleting an analysis must cascade to child records in one transaction.

---

## 14. Data model

Use Room for structured local persistence. The exact schema may vary, but it must represent the following concepts.

### 14.1 `AnalysisEntity`

- `id: UUID`
- `displayName: String`
- `sourceFileName: String?`
- `sha256: String`
- `fileSizeBytes: Long`
- `createdAt: Instant`
- `completedAt: Instant?`
- `durationMs: Long?`
- `status: AnalysisStatus`
- `packageName: String?`
- `applicationLabel: String?`
- `versionName: String?`
- `versionCode: Long?`
- `minSdk: Int?`
- `targetSdk: Int?`
- `compileSdk: Int?`
- `engineVersion: String`
- `ruleSetVersion: String`
- `fingerprintVersion: String`
- `completenessFlags: Long or structured value`
- `warningCount: Int`
- `retainedApkPath: String?`

### 14.2 Supporting entities

- `AnalysisStageEntity`
- `PermissionEntity`
- `CustomPermissionEntity`
- `ComponentEntity`
- `IntentFilterEntity`
- `IntentFilterDataEntity`
- `ManifestMetadataEntity`
- `SignerEntity`
- `SigningLineageEntity`
- `ArchiveEntryEntity`
- `ArchiveAggregateEntity`
- `DexFileEntity`
- `NativeLibraryEntity`
- `SdkDetectionEntity`
- `SdkEvidenceEntity`
- `FindingEntity`
- `FindingEvidenceEntity`
- `AnalysisWarningEntity`

### 14.3 Storage rules

- Use stable string enums with converters or integer enums with explicit mapping.
- Never persist Android `Parcelable` objects.
- Do not persist exception stack traces in normal production data.
- Database migrations are mandatory; destructive migration is prohibited in release builds.
- Export Room schemas to the repository.
- Every migration must have an instrumented migration test.

---

## 15. Security, privacy, and hostile-input requirements

### 15.1 Application permissions

The release application should require no dangerous runtime permissions. It must not request `INTERNET`, broad storage permissions, package installation, package enumeration, or accessibility privileges.

Use the Storage Access Framework for user-selected files and app-private storage for temporary data.

### 15.2 No code execution

Prohibited operations include:

- Installing the selected APK.
- Launching activities from the selected APK.
- Loading DEX or native libraries.
- Reflectively invoking APK classes.
- Running embedded scripts.
- Rendering APK-provided HTML in a WebView.

### 15.3 ZIP safety

- Never concatenate untrusted entry names into extraction paths.
- Do not extract the APK tree.
- Normalize and validate entry names.
- Detect duplicate critical entries.
- Bound entry count and decompressed bytes.
- Avoid trusting `ZipEntry.size` without enforcing actual read limits.
- Use `Long` for byte sizes and overflow-safe arithmetic.
- Catch malformed central-directory and compression exceptions.

### 15.4 Parser isolation

Every parser should:

- Accept an input abstraction or bounded stream.
- Return typed results and warnings.
- Avoid global mutable state.
- Enforce local limits.
- Be fuzz-testable.
- Never log raw binary content.

A separate Android process is optional post-MVP. For MVP, resilience must come from strict limits, cancellation, defensive parsers, and crash-free error handling.

### 15.5 Logging

- Use structured internal logging in debug builds.
- Redact full source URIs and local filesystem paths.
- Do not log manifest contents, certificate serials, or package names by default in release builds.
- Do not include analytics or remote crash reporting.
- Provide an optional user-initiated local diagnostic export containing app logs only after redaction.

### 15.6 Supply-chain controls

- Enable Gradle dependency verification.
- Commit dependency-locking information where supported.
- Run dependency and license checks in CI.
- Use Dependabot or Renovate for update proposals.
- Do not automatically merge dependency upgrades.
- Document every non-AndroidX parsing dependency and its license.

---

## 16. Performance and reliability requirements

### 16.1 Threading

- No file I/O, ZIP parsing, hashing, database bulk writes, DEX scanning, or report generation on the main thread.
- Use explicit dispatchers injected through a `DispatcherProvider`.
- Limit parallel analyzers to avoid memory pressure.
- Use sequences/flows carefully; do not create unbounded buffers.

### 16.2 Memory

- Stream file import and hashing.
- Do not read the complete APK into memory.
- Do not decode all archive files.
- Use compact class-prefix representations for SDK detection.
- Batch Room inserts.
- Release parser resources promptly.

Target peak private memory during analysis of the 500 MB fixture: no more than 256 MiB above idle on the reference device. If this target cannot be met, document measured behavior and optimize before release.

### 16.3 Responsiveness

- Initial progress UI appears within 500 ms after a document is returned.
- Cancel action remains responsive.
- Long lists use lazy layouts and stable keys.
- Expensive filtering or sorting occurs off the main thread when needed.
- Process recreation restores active-job and navigation state from persisted data.

### 16.4 Recovery

- If the process dies after import, the job may resume or restart from the private APK copy.
- If the app updates while a job is active, it must not expose a corrupt completed analysis.
- Startup reconciles jobs stuck in non-terminal states and marks unrecoverable jobs failed with an explanation.

---

## 17. Testing strategy

## 17.1 Development method

Use test-driven development whenever the behavior can be expressed before implementation, especially for:

- Binary parsing.
- Safety-limit enforcement.
- Effective-export computation.
- Rule evaluation.
- SDK detection.
- Comparison.
- Report generation.
- Database migrations.

Required workflow:

1. Add or update a failing test.
2. Implement the smallest correct behavior.
3. Refactor while tests remain green.
4. Add edge and malformed-input cases.
5. Run the relevant local suite before committing.

Every defect fixed after discovery must include a regression test that fails without the fix.

## 17.2 Test pyramid

Target distribution by test count and execution frequency:

- Approximately 70% local unit tests.
- Approximately 20% integration tests.
- Approximately 10% instrumented/UI/end-to-end tests.

These are guidance, not a reason to avoid a needed instrumented test.

## 17.3 Test source sets

- `src/test`: JVM unit and integration tests that do not require a device.
- `src/androidTest`: tests requiring Android framework, Room migration helpers, Compose UI, PackageManager, SAF behavior, or a device/emulator.
- `:benchmark`: Macrobenchmark and Baseline Profile generation.
- `:fixtures`: deterministic APK-producing fixture projects or build tasks.

## 17.4 Unit tests

### 17.4.1 Import and validation

Test:

- Hash calculation with known vectors.
- Exact-size boundary acceptance and rejection.
- Unknown source length.
- Cancellation during copy.
- Partial-file deletion.
- Invalid ZIP magic.
- Missing manifest.
- Duplicate critical entry.
- Entry-count limit.
- Integer overflow attempts.
- Path traversal and unusual Unicode entry names.
- Declared size different from bytes read.

Use fake streams that throw at controlled offsets.

### 17.4.2 Manifest parser

Test:

- Minimal manifest.
- Namespaced attributes.
- All supported typed values.
- Resource references.
- Missing string-pool items.
- Unknown chunks.
- Truncated chunks.
- Invalid chunk lengths.
- Duplicate attributes.
- Very deep XML nesting with a safe depth limit.
- Every supported component and intent-filter field.

Use golden decoded output and typed-model assertions. Golden files must be reviewed and stable.

### 17.4.3 Effective exported-state calculation

Create a table-driven matrix covering:

- Every component type.
- Explicit true/false/absent.
- With and without intent filters.
- Relevant target SDK boundaries.
- Provider-specific defaults.
- Activity aliases.
- Unknown target SDK.

The explanation string or explanation code must also be tested.

### 17.4.4 Permission analysis

Test:

- Platform permission lookup.
- Custom permissions.
- Protection-level mapping.
- Unknown permissions.
- SDK qualifiers.
- Sensitive-category mapping.
- Permission combinations.

### 17.4.5 Rule engine

Every rule must have tests for:

- Positive trigger.
- Negative/non-trigger.
- Not applicable.
- Missing required fact returns `Unknown`.
- Evidence correctness.
- Severity and confidence.
- Stable rule ID.
- False-positive exception when applicable.

Use parameterized tests for rule matrices.

### 17.4.6 SDK detection

Test:

- Exact class match.
- Prefix match.
- Generic-prefix collision.
- Multi-signal confidence boost.
- Negative indicator.
- Multidex evidence.
- Duplicate evidence deduplication.
- Unknown SDK.
- Fingerprint JSON schema validation.
- Duplicate IDs and invalid weights.

### 17.4.7 Archive and size

Test:

- Category classification.
- Folder aggregation.
- Compressed/uncompressed totals.
- Unknown sizes.
- Overflow-safe percentage calculation.
- Top-N ordering.
- Equal-size deterministic tie handling.
- Expansion-ratio policy.

### 17.4.8 Comparison engine

Test:

- Added, removed, changed, and unchanged items.
- Semantic key matching.
- Same package versus different package.
- Signing lineage rotation versus unrelated signer.
- New sensitive permission.
- New exported component.
- Size threshold boundaries.
- Incomplete source analysis producing unknown diff.
- Deterministic ordering.

### 17.4.9 Reports

Test:

- Markdown golden output.
- HTML escaping of APK-controlled values.
- JSON schema conformance.
- Stable field names.
- Null handling.
- Deterministic ordering.
- Optional archive inventory.
- Disclaimer presence.
- No source URI or local path leakage by default.

### 17.4.10 ViewModels and reducers

Test:

- Initial state.
- Loading, content, empty, partial, and error states.
- Filter/search changes.
- Cancellation event.
- One-time navigation and Snackbar events.
- State restoration inputs.
- No duplicate event after re-collection.

Use fake repositories and a test coroutine scheduler.

## 17.5 JVM integration tests

Integration tests should combine real implementations without a device where possible.

Required integration suites:

1. Import stream -> private test file -> validator -> hash.
2. Real ZIP fixture -> archive inventory -> manifest parser.
3. Real DEX fixture -> DEX metrics -> SDK detector.
4. Parsed facts -> complete rule engine.
5. Two analysis snapshots -> comparison engine.
6. Analysis snapshot -> Markdown/HTML/JSON generators.
7. Repository using an in-memory or temporary SQLite/Room-compatible test setup where reliable.
8. Cancellation across multi-stage pipeline.
9. Partial parser failure that still yields `COMPLETED_WITH_WARNINGS`.

Do not mock the class under test's direct collaborators when an inexpensive deterministic fake or real in-memory implementation provides more confidence.

## 17.6 Fixture APK strategy

Create deterministic fixture applications inside the repository. They are test inputs, not production features.

Required fixtures:

- `fixture-minimal-safe`: minimal manifest, no risky flags.
- `fixture-debuggable`: debuggable and test-only flags.
- `fixture-exported-components`: exported activity, service, receiver, and provider with protected and unprotected variants.
- `fixture-permissions`: normal, dangerous, special, custom weak, and custom strong permissions.
- `fixture-intent-filters`: launcher, browsable deep link, custom action, provider authorities.
- `fixture-multidex`: multiple DEX files and recognizable package prefixes.
- `fixture-native`: 32-bit and 64-bit native libraries.
- `fixture-sdk-fingerprints`: fake SDK namespaces designed solely for deterministic tests.
- `fixture-large-assets`: controlled size thresholds without committing huge binaries.
- `fixture-signing-a` and `fixture-signing-b`: same package signed with different test keys.
- `fixture-signing-rotation`: where supported by the test toolchain.
- `fixture-target-sdk-matrix`: manifests for relevant SDK-boundary behavior.

Fixture signing keys must be test-only and clearly labelled. Never use release credentials.

Malformed archives should be generated by test code to avoid committing large or dangerous binaries. Include:

- Truncated central directory.
- Duplicate manifest entries.
- Invalid compression metadata.
- Oversized declared entry.
- Too many entries with tiny contents.
- Invalid AXML chunks.
- Unsupported/truncated DEX.

## 17.7 Android integration and instrumentation tests

Use `AndroidJUnitRunner` and Compose testing APIs.

### 17.7.1 Storage Access Framework

Test with a dedicated test `DocumentsProvider` or `ContentProvider` that can return:

- Valid APK content.
- Unknown length.
- Delayed reads.
- Read failure at a chosen offset.
- Revoked permission.
- Misleading filename and MIME type.

Validate that import works from a `content://` URI and never assumes a filesystem path.

### 17.7.2 PackageManager archive inspection

On device/emulator:

- Copy fixture APKs to app-private storage.
- Call the real archive APIs.
- Verify metadata mapping.
- Verify icon/label loading behavior.
- Verify signing-certificate availability on supported APIs.
- Compare PackageManager facts with the custom manifest parser and surface discrepancies as warnings.

### 17.7.3 Room database

Instrumented tests must cover:

- DAO insert/query/delete.
- Cascading deletion.
- Transactional publication of a completed analysis.
- Active job recovery.
- Every schema migration using exported schemas and `MigrationTestHelper`.
- No destructive fallback in release configuration.

### 17.7.4 Compose UI tests

Required user-flow tests:

1. Home -> analyze action -> selected fixture -> progress -> result summary.
2. Cancel active analysis.
3. Open each result destination.
4. Filter findings by severity.
5. Search permissions/components/files.
6. Open evidence detail.
7. Select two history items and compare.
8. Export report using a fake document contract boundary.
9. Delete analysis with confirmation.
10. Empty history and error states.
11. Process/activity recreation during analysis and on result screens.
12. Compact and expanded window layouts.

Use semantic test tags sparingly; prefer user-visible semantics and text when stable.

### 17.7.5 Accessibility tests

Automate checks for:

- Missing content descriptions.
- Clickable elements without roles or labels.
- Severity exposed only through color.
- Truncated primary actions at large font scale.
- Logical focus order on critical screens.

Manual TalkBack review remains required before release.

## 17.8 End-to-end tests

At least one end-to-end instrumentation test must:

1. Import a real fixture through a content URI.
2. Run the real analysis worker.
3. Persist the result.
4. Render the summary.
5. Open a finding and its evidence.
6. Generate a JSON report.
7. Validate key report fields.

The test must use no network and must be deterministic.

## 17.9 Performance tests

Create Macrobenchmarks for:

- Cold startup to first interactive home screen.
- Opening history containing at least 100 analyses.
- Opening a completed analysis.
- Scrolling a findings list of at least 200 items.
- Scrolling an archive list of at least 10,000 rows or a representative paged dataset.
- Analysis of a small and medium deterministic fixture.

Measure where supported:

- Startup time.
- Frame timing/jank.
- Analysis duration.
- Peak memory using repeatable profiling or benchmark instrumentation.

Performance tests must run on a stable emulator/device profile. Do not fail normal pull requests on noisy micro-differences; use broad regression thresholds and scheduled benchmark jobs.

## 17.10 Baseline Profiles

Generate a Baseline Profile covering:

- App startup.
- Home rendering.
- Opening history.
- Opening an analysis summary.
- Navigating to findings and size views.
- Scrolling the main result lists.

Use Macrobenchmark to compare startup and critical journeys with and without the profile. Commit the generated profile according to Android guidance.

## 17.11 Fuzz and property-based tests

At minimum, use property-based or fuzz-style tests for:

- AXML chunk lengths and string pools.
- ZIP entry names and size arithmetic.
- Report HTML escaping.
- SDK prefix matching.
- Diff symmetry and determinism.

Properties should include:

- Parser never hangs on bounded input.
- Parser either returns a result or a typed failure.
- Comparison of A with A has no changes.
- Comparing A to B and B to A reverses added/removed classifications.
- Report generation never emits unescaped APK-controlled HTML.

## 17.12 Coverage and quality gates

Minimum CI thresholds:

- Rule engine: 90% line, 85% branch.
- Comparison engine: 90% line, 85% branch.
- Manifest model/parser code that is reasonably measurable: 85% line.
- Report generators: 90% line.
- Overall non-generated production code: 80% line.

Coverage exclusions must be documented and limited to generated code, Compose previews, DI wiring, and unavoidable Android entry points.

Coverage is not a substitute for fixture quality, boundary tests, or instrumented tests.

---

## 18. CI/CD requirements

Use GitHub Actions.

### 18.1 Pull-request pipeline

Required jobs:

1. **Build fixture APKs**.
2. **Formatting and static analysis**: Spotless or ktfmt, Detekt, Android Lint.
3. **Unit and JVM integration tests**.
4. **Coverage verification**.
5. **Debug and release compilation**.
6. **Dependency verification and license check**.
7. **JSON schema and SDK fingerprint validation**.
8. **Report golden-file consistency**.

### 18.2 Instrumented pipeline

Run instrumented tests on:

- API 26 emulator.
- Latest stable API emulator.

If execution time is excessive, run a smoke subset on every pull request and the full matrix nightly and before releases. The branch-protection policy must still require a recent successful full run for release.

### 18.3 Benchmark pipeline

Run Macrobenchmarks and Baseline Profile generation:

- On a scheduled workflow.
- Before tagged releases.
- After significant Compose, database, or parser changes.

Store benchmark JSON and trace artifacts for inspection.

### 18.4 Release pipeline

For GitHub releases:

- Build a reproducible release APK.
- Generate checksums.
- Generate changelog from conventional commits or curated release notes.
- Attach APK, checksum, sample report, and SBOM if implemented.
- Never place signing secrets in the repository.
- Use GitHub encrypted secrets or a documented local signing process.

---

## 19. Static analysis and code quality

Required:

- Android Lint with warnings reviewed; selected critical checks treated as errors.
- Detekt with a committed configuration.
- Consistent formatting.
- Explicit API mode for reusable engine modules if practical.
- No ignored coroutine exceptions.
- No `GlobalScope`.
- No blocking I/O on Main.
- No broad `catch (Throwable)` except at a top-level job boundary that rethrows cancellation and maps unexpected failures.
- No force unwraps in parser code unless a preceding invariant check is obvious and tested.
- No TODO/FIXME in release-critical paths.

---

## 20. Documentation requirements

The repository must include:

- `README.md` with screenshots, demo GIF/video link, capabilities, privacy promise, limitations, build steps, and architecture overview.
- `PRD.md` containing this specification.
- `ARCHITECTURE.md`.
- `TESTING.md` with local, instrumented, fixture, benchmark, and CI instructions.
- `SECURITY.md` with vulnerability reporting and hostile-file policy.
- `PRIVACY.md` stating that no data leaves the device.
- `RULES.md`.
- `SDK_FINGERPRINTS.md`.
- `CONTRIBUTING.md`.
- `CHANGELOG.md`.
- Third-party notices and licenses.
- Architecture Decision Records under `docs/adr/`.
- JSON report schema under `docs/schema/`.
- At least one sample Markdown, HTML, and JSON report generated from a repository fixture.

Recommended ADRs:

1. Why APK X-Ray has no Internet permission.
2. Binary manifest parser selection.
3. Signature inspection strategy and limitations.
4. DEX parser selection.
5. Why the product avoids a single security score.
6. Why APK bytes are not retained by default.
7. WorkManager/background execution strategy.
8. Rule and SDK fingerprint versioning.

---

## 21. AI implementation instructions

The AI coding agent must follow these rules.

### 21.1 General behavior

1. Treat this PRD as normative.
2. Do not silently omit a requirement.
3. When a requirement is technically infeasible, document the issue in `IMPLEMENTATION_STATUS.md`, implement the safest truthful fallback, and add a test for the fallback.
4. Do not claim successful signature verification when only certificate extraction succeeded.
5. Prefer small, reviewable commits and preserve a buildable main branch.
6. Run relevant tests after every meaningful change.
7. Never disable a failing test merely to make CI green.
8. Record assumptions and decisions in ADRs.
9. Use stable dependencies and pin versions.
10. Keep the release manifest free of `INTERNET`.

### 21.2 Implementation phases

#### Phase 0 — Repository foundation

- Create modules, version catalog, CI skeleton, formatting, Detekt, Lint, dependency verification, test utilities, and documentation structure.
- Add a minimal Compose shell.
- Add a test that asserts the merged release manifest does not request `INTERNET`.

**Exit criteria:** Project builds, static checks run, unit tests run, CI passes.

#### Phase 1 — Import and validation

- Implement SAF selection boundary.
- Stream to private storage with SHA-256, limits, progress, and cancellation.
- Implement ZIP validation and typed errors.
- Add valid and malformed fixtures.

**Exit criteria:** Import/validation test suite passes and UI displays success/error states.

#### Phase 2 — Manifest and metadata

- Implement AXML parsing.
- Build typed manifest model.
- Add PackageManager cross-check.
- Implement summary, manifest, permissions, and components UI.

**Exit criteria:** Fixture manifests produce expected facts and instrumented archive inspection works.

#### Phase 3 — Archive, DEX, native, and SDK detection

- Implement archive inventory and size aggregation.
- Add DEX metrics and class-prefix scanning.
- Add native ABI inventory.
- Add fingerprint database and SDK detector.

**Exit criteria:** Multidex/native/SDK fixtures pass and memory remains bounded.

#### Phase 4 — Signing and findings

- Implement certificate inspection.
- Complete signature-verification compatibility spike and truthful fallback.
- Implement initial rule set and finding UI.

**Exit criteria:** Every rule has positive, negative, and unknown tests; signing status wording is accurate.

#### Phase 5 — Persistence, history, and comparison

- Persist immutable completed snapshots.
- Implement history and deletion.
- Implement semantic diff and important-change findings.

**Exit criteria:** Migration, DAO, comparison, and UI tests pass.

#### Phase 6 — Reports and sharing

- Implement Markdown, HTML, and JSON generation.
- Add schema validation and escaping tests.
- Implement save/share flows.

**Exit criteria:** Golden reports and end-to-end report test pass.

#### Phase 7 — Polish and release engineering

- Adaptive layouts.
- Accessibility pass.
- Baseline Profile.
- Macrobenchmarks.
- Screenshots and sample reports.
- Complete documentation, licenses, and release workflow.

**Exit criteria:** All definition-of-done requirements pass.

### 21.3 Status tracking

Maintain `IMPLEMENTATION_STATUS.md` with a table containing:

- Requirement ID.
- Status: not started, in progress, implemented, tested, deferred.
- Source files.
- Test files.
- Notes or limitations.

Update it in the same commit as requirement implementation.

---

## 22. Acceptance criteria by capability

### 22.1 Import

- A valid APK selected from a `content://` URI is copied, hashed, validated, and analyzed.
- A misleading extension or MIME type does not bypass validation.
- Over-limit and malformed inputs return typed errors.
- Cancellation removes partial files.

### 22.2 Manifest and components

- Final manifest is decoded and searchable.
- Permissions and all required component types are displayed.
- Intent filters are available from the custom parser.
- Effective exported state is accompanied by an explanation.

### 22.3 Signing

- Certificate fingerprints are displayed when obtainable.
- Full verification status is shown only when actually performed.
- Unknown/unavailable status is explicit.
- Unrelated signer change between same-package APKs is highlighted.

### 22.4 SDK detection

- Fixture SDKs are detected at expected confidence.
- Generic-prefix false positives are controlled.
- Evidence is visible.
- Fingerprint database validates in CI.

### 22.5 Findings

- All initial rules exist with stable IDs.
- Every finding has evidence, confidence, and remediation.
- Missing facts produce unknown results, not guessed findings.
- No aggregate safety score exists.

### 22.6 Comparison

- Added, removed, and changed permissions/components/SDKs/signers/size are correct.
- Important changes are highlighted.
- Different-package comparison shows a warning.

### 22.7 Reports

- Markdown, HTML, and JSON export successfully.
- HTML safely escapes APK-controlled content.
- JSON validates against the committed schema.
- Reports contain hash and engine/rule/fingerprint versions.

### 22.8 Privacy

- Release manifest has no `INTERNET` permission.
- No analytics or remote crash SDK is present.
- APK and report contents remain local unless the user explicitly shares or saves them.

---

## 23. Definition of done

The MVP is done only when:

1. All MVP functional requirements are implemented or explicitly deferred with an approved reason.
2. All acceptance criteria pass.
3. Pull-request and full instrumented CI are green.
4. Coverage thresholds pass.
5. No release-critical Lint or Detekt errors remain.
6. Database migrations are tested.
7. Baseline Profile is generated and measured.
8. No Internet permission exists in the release manifest.
9. Accessibility review is complete.
10. A hostile-input manual test set completes without app crashes or hangs.
11. Sample reports and screenshots are committed.
12. README, architecture, testing, privacy, security, rules, fingerprints, licenses, and ADRs are complete.
13. A tagged GitHub release can be built reproducibly from documented instructions.

---

## 24. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Malformed APK crashes parser | High | Bounded parsers, typed errors, fuzz tests, hostile fixtures. |
| ZIP bomb or huge APK exhausts resources | High | Hard limits, streaming, no extraction, bounded reads, cancellation. |
| Signature library is not Android-compatible | High | Compatibility spike, public PackageManager fallback, truthful status wording. |
| SDK detection produces false positives | Medium | Multi-signal scoring, confidence, evidence, negative indicators, collision tests. |
| Findings are interpreted as malware verdicts | High | No safety score, repeated disclaimer, evidence and confidence, neutral wording. |
| Database becomes too large | Medium | Archive-entry limits, storage screen, cascade deletion, optional detail retention policy. |
| Long analysis is killed by OS | Medium | WorkManager/lifecycle-resilient jobs, persisted state, recovery logic. |
| Third-party parser dependency becomes abandoned | Medium | Isolate behind interfaces, pin version, ADR, fixture suite enabling replacement. |
| UI becomes too dense | Medium | Progressive disclosure, search/filter, adaptive list-detail layouts. |
| Android platform behavior changes | Medium | PackageManager cross-check, target latest stable, API matrix tests, versioned policies. |

---

## 25. Future roadmap

### Version 1.1

- Share-target import.
- User-selectable report sections.
- Better archive-tree paging.
- Additional SDK fingerprints.
- Optional local diagnostic bundle.

### Version 1.2

- APK-set and split APK analysis.
- AAB analysis where technically feasible.
- More detailed ELF metadata.
- Resource-table inspection.
- SBOM export.

### Version 2.0

- Safe declarative custom rules.
- Local secret-pattern scanning.
- Desktop parser companion.
- Reusable open-source Kotlin analysis engine.

---

## 26. Normative technical references

These references guide implementation but do not override the explicit requirements in this PRD.

1. Android Storage Access Framework — opening documents:  
   https://developer.android.com/training/data-storage/shared/documents-files
2. Android package archive inspection with `PackageManager`:  
   https://developer.android.com/reference/android/content/pm/PackageManager
3. Android package visibility behavior:  
   https://developer.android.com/training/package-visibility
4. Android app architecture guidance:  
   https://developer.android.com/topic/architecture
5. Android testing fundamentals:  
   https://developer.android.com/training/testing/fundamentals
6. Local unit tests:  
   https://developer.android.com/training/testing/local-tests
7. Instrumented tests:  
   https://developer.android.com/training/testing/instrumented-tests
8. Compose UI testing:  
   https://developer.android.com/develop/ui/compose/testing
9. Room testing and migrations:  
   https://developer.android.com/training/data-storage/room/testing-db
10. Macrobenchmark and Baseline Profiles:  
    https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview  
    https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile
11. Android Studio APK Analyzer capabilities:  
    https://developer.android.com/studio/debug/apk-analyzer
12. AOSP `apksig`:  
    https://android.googlesource.com/platform/tools/apksig/
13. Google's maintained smali/dexlib2 fork:  
    https://github.com/google/smali

---

## 27. Product disclaimer text

Use wording substantially equivalent to the following in onboarding, About, and reports:

> APK X-Ray performs static inspection of selected APK files entirely on this device. Findings are automated risk indicators based on package contents and configuration. They do not prove that an app is malicious, vulnerable, safe, or compliant, and they are not a substitute for a complete security review or runtime analysis.

---

**End of PRD**
