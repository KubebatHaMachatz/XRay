# Implementation status

This tracks progress against `APK_X-Ray_PRD.md`. The PRD describes a full 8-phase, multi-module,
production-grade Android application (custom AXML/DEX parsers, SDK fingerprinting, signature
verification, Room-backed history, comparison engine, report generation, CI, fuzzing, benchmarks,
Baseline Profiles, and a full documentation set). That is realistically weeks of engineering work.

**This milestone implements PRD Phase 0 through Phase 2** (repository foundation, import/validation,
manifest/metadata analysis) plus a working subset of Phase 4's rule engine (the manifest- and
archive-derived rules only, since signing/DEX/SDK detection are not yet implemented). The result is
a real, buildable, installable app that performs the core "select an APK → see facts and findings"
journey end to end, entirely offline, with unit-tested parsing/rule logic.

Everything else in the PRD is explicitly deferred, not silently dropped. See "Deferred" below.

## Status legend

`implemented` = built and unit-tested · `partial` = a working subset exists · `deferred` = not
started, intentionally out of scope for this milestone.

## Phase 0 — Repository foundation

| Item | Status | Notes |
|---|---|---|
| Multi-module structure | implemented | `:app`, `:core:model`, `:core:common`, `:engine:apk`, `:engine:rules`. PRD's full module list (`:core:designsystem`, `:core:database`, `:core:files`, `:core:testing`, `:engine:report`, `:feature:*`, `:benchmark`, `:fixtures`) is not yet split out — see Deferred. |
| Version catalog (`gradle/libs.versions.toml`) | implemented | All dependency versions pinned; no dynamic versions. |
| Minimal Compose shell | implemented | Full navigation-by-state Compose UI (Home → Progress → Result), not just a shell. |
| Test asserting release manifest has no `INTERNET` | implemented | `app/src/test/java/com/demo/xray/ManifestNoInternetPermissionTest.kt`. Verified manually against both the debug and release merged manifests during this session. |
| CI, Detekt, Lint config, dependency verification | deferred | See Deferred. |

## Phase 1 — Import and validation (FR-001–FR-006, FR-010–FR-013)

| Requirement | Status | Source | Notes |
|---|---|---|---|
| FR-001 SAF `OpenDocument` selection | implemented | `app/src/main/java/com/demo/xray/MainActivity.kt` | |
| FR-002 `ACTION_SEND` shared APK | deferred | | |
| FR-003 Streamed import + incremental SHA-256 | implemented | `engine/apk/.../ApkImporter.kt` | Bounded buffer, no whole-file buffering. |
| FR-004 Input limits | implemented | `core/common/.../AnalysisLimits.kt` | All limits are named constants with boundary tests. |
| FR-005 ZIP/manifest structural validation | implemented | `engine/apk/.../ApkValidator.kt` | Magic/central-directory validity, duplicate-manifest rejection, path-traversal flagging, entry-count limit. Does not extract the archive tree. |
| FR-006 URI permission retention | deferred | | No persistent URI retention exists at all (no "keep source link" setting); URI is only used transiently during import. |
| FR-010 Persistent `AnalysisJob` (WorkManager-backed) | partial | `core/model/.../AnalysisJob.kt`, `app/.../domain/AnalysisPipeline.kt` | `AnalysisJobState` enum and `AnalysisProgress` model exist and are used, but the job runs in a plain `ViewModel`-scoped coroutine, not WorkManager — it does **not** survive process death. See ADR-0003. |
| FR-011 Progress model | implemented | `AnalysisPipeline.kt` | Stage + description + elapsed time; overall percent only during import (other stages are not weighted/estimated). |
| FR-012 Cooperative cancellation | implemented | `ApkImporter.kt`, `AnalysisPipeline.kt` | Checked during import copy; not yet threaded through manifest/rule stages (they are fast enough in this milestone's scope that this is a minor gap, but should be added once DEX analysis exists). |
| FR-013 Typed error model | implemented | `core/model/.../AnalysisError.kt` | All listed error types modeled. |

## Phase 2 — Manifest and metadata (FR-020–FR-033)

| Requirement | Status | Source | Notes |
|---|---|---|---|
| FR-020 Package summary | partial | `ui/screens/result/OverviewTab.kt` | Covers package/version/SDKs/hash/size/date/duration/engine+rule version/status/counts. Missing: app icon (see below), signing summary (signing not implemented). |
| FR-021 Field source/confidence | partial | `FieldSource` enum exists in `core/model` but is not yet attached per-field in the UI. |
| FR-022 `PackageManager` cross-check | implemented | `app/.../data/PackageManagerCrossChecker.kt` | Label cross-check only; icon loading deferred (see below) since the private APK copy is deleted right after analysis per FR-140, and threading a `Bitmap` through the pure `AnalysisResult` model was out of scope for this pass. |
| FR-030 Binary AXML parser | implemented | `engine/apk/.../axml/*.kt` | Hand-written per the PRD's explicit allowance ("...or implement a focused parser with comprehensive golden and malformed-input tests"). String pool (UTF-8 and UTF-16), resource map (skipped, not needed for name resolution), namespace/element/attribute chunks, typed value rendering, iterative (non-recursive) tree building with a depth limit, bounds-checked reads that always yield a typed `MalformedManifest` error instead of crashing. 12 tests including truncated/unbalanced/deeply-nested hostile input. |
| FR-031 Canonical raw XML viewer | deferred | | `ManifestTab.kt` shows a structured facts summary instead of a searchable raw XML tree. Reconstructing canonical XML text would require retaining the `AxmlDocument` (currently discarded after being converted to facts) and a dedicated renderer. |
| FR-032 Parsed manifest facts | implemented | `engine/apk/.../manifest/ManifestAnalyzer.kt` | `<uses-sdk>`, `<uses-permission>`, `<permission>`, `<uses-feature>`, `<queries>`, `<application>`, all component types, `<intent-filter>`, `<meta-data>`, `<uses-library>`, `<instrumentation>`. |
| FR-033 Effective exported-state | implemented | `engine/apk/.../manifest/EffectiveExportCalculator.kt` | Pure, table-driven, 21 parameterized test cases across every component type, explicit true/false/absent, intent-filter presence, and the provider API-17 default-flip boundary including unknown-SDK → `UNKNOWN`. |

## Rule engine (FR-110–FR-114) — partial

24 of the PRD's ~30 initial rules are implemented: every rule computable from manifest and archive
facts alone (`APP_DEBUGGABLE`, `APP_TEST_ONLY`, `CLEARTEXT_TRAFFIC_ALLOWED`,
`EXPORTED_PROVIDER_UNPROTECTED`, `EXPORTED_SERVICE_UNPROTECTED`, `EXPORTED_RECEIVER_UNPROTECTED`,
`EXPORTED_ACTIVITY_REVIEW`, `WEAK_CUSTOM_PERMISSION_GUARD`, `SHARED_USER_ID_DECLARED`,
`BACKUP_CONFIGURATION_REVIEW`, `LEGACY_EXTERNAL_STORAGE`, `QUERY_ALL_PACKAGES_REQUESTED`,
`REQUEST_INSTALL_PACKAGES`, `SYSTEM_ALERT_WINDOW_REQUESTED`, `ACCESSIBILITY_SERVICE_DECLARED`,
`BACKGROUND_LOCATION_REQUESTED`, `SMS_OR_CALL_LOG_PERMISSION`, `SENSITIVE_PERMISSION_COMBINATION`,
`TARGET_SDK_BELOW_POLICY`, `ONLY_32_BIT_NATIVE_LIBS`, `NATIVE_LIBRARY_ABI_MISMATCH`,
`LARGE_UNCOMPRESSED_ENTRY`, `EXTREME_ZIP_EXPANSION_RATIO`, `DUPLICATE_CRITICAL_ENTRY`). See
`engine/rules/src/main/kotlin/com/demo/xray/engine/rules/builtin/`. Every rule has positive,
negative, and (where applicable) `Unknown` tests. `RuleRegistry` fails fast on duplicate IDs.

Deferred (require features not yet built): `SIGNATURE_VERIFICATION_FAILED`,
`SIGNATURE_STATUS_UNKNOWN` (need signing inspection), `SIGNER_CHANGED_IN_COMPARISON`,
`NEW_SENSITIVE_PERMISSION`, `NEW_EXPORTED_COMPONENT`, `NEW_TRACKING_SDK` (need the comparison
engine and SDK detection). `RULES.md` (FR-114) is not yet written.

FR-111 (no aggregate security score) is honored: the UI shows counts by severity only.

## Deferred entirely (not started)

These are large, independent PRD sections that were out of scope for this pass:

- **Signing/certificate inspection** (11.7) — no `apksig` compatibility spike performed yet.
- **DEX analysis** (11.9) — no dexlib2/smali integration.
- **Embedded SDK detection** (11.10) — no fingerprint database.
- **Native library ELF inspection** (11.11) — archive inventory categorizes `lib/<abi>/` entries and a couple of ABI-consistency rules run off that, but there is no ELF header parsing.
- **Room persistence / history / comparison** (11.14, 11.15, §14) — `AnalysisResult` lives only in the `ViewModel`'s in-memory `StateFlow`; closing the app loses it. No database, no history list, no compare-two-APKs flow.
- **Reports (Markdown/HTML/JSON export/share)** (11.16) — no report generators, no `docs/schema/`.
- **Settings screen** (11.17 FR-160) — no theme/retention/threshold settings UI (the one behavioral default from that section — "keep APK copies: off by default" — is honored in `AnalysisPipeline`, which always deletes the imported file).
- **About/transparency screen** (FR-161) — the disclaimer text is shown on the home screen only.
- **App icon display** (part of FR-020) — see the FR-022 note above.
- **Global/local search and filtering** beyond the Findings-screen severity filter (FR-042, FR-052, FR-122).
- **Adaptive list-detail layouts for expanded screens** (12.3) — only compact single-pane layouts exist.
- **Accessibility automation, TalkBack manual pass** (12.2, 17.7.5).
- **WorkManager-based lifecycle-resilient background execution** (FR-010) — see the Phase 1 table above.
- **Instrumented/Compose UI tests, migration tests, end-to-end tests, Macrobenchmarks, Baseline Profiles** (17.5–17.10) — only JVM unit tests exist (103 across `core:common`, `engine:apk`, `engine:rules`, and `app`).
- **Fuzz/property-based tests** (17.11).
- **CI/CD, static analysis config (Detekt/Lint gates), dependency verification, coverage gates** (18, 19).
- **Documentation set**: `README.md`, `ARCHITECTURE.md`, `TESTING.md`, `SECURITY.md`, `PRIVACY.md`, `RULES.md`, `SDK_FINGERPRINTS.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, `docs/adr/*`. (`APK_X-Ray_PRD.md` at the repo root already satisfies the "PRD.md" requirement.)

## Verified this session

- `./gradlew assembleDebug` — succeeds.
- `./gradlew assembleRelease` — succeeds, including `lintVitalRelease`.
- `./gradlew test` — all JVM unit tests pass (103 tests: 6 in `core:common`, 54 in `engine:apk`, 42 in `engine:rules`, 1 in `app`).
- Merged debug **and** release manifests manually inspected: no `android.permission.INTERNET` entry in either.
- Template leftover files (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`) removed; `app/src/androidTest` is now empty pending real instrumented tests.

## Known cleanup items for the next pass

- `FieldSource` (FR-021) is modeled but unused; either wire it into the UI or remove it until it is.
