package com.demo.xray

import com.demo.xray.core.common.DefaultDispatcherProvider
import com.demo.xray.core.common.DispatcherProvider
import com.demo.xray.core.common.Sha256HashCalculator
import com.demo.xray.core.common.SystemClock
import com.demo.xray.domain.AnalysisPipeline
import com.demo.xray.engine.apk.ApkImporter
import com.demo.xray.engine.apk.ApkValidator
import com.demo.xray.engine.apk.DefaultApkImporter
import com.demo.xray.engine.apk.DefaultApkValidator
import com.demo.xray.engine.rules.DefaultFindingsEngine
import com.demo.xray.engine.rules.FindingsEngine

/**
 * Manual dependency injection container for the application.
 *
 * **Design rationale** (see ADR-0004):
 *  - Hand-rolled instead of Hilt to avoid annotation processing build overhead for this milestone
 *  - Every dependency is explicitly constructor-injected and swappable in tests
 *  - Hilt can be added later without changing the architecture (would just auto-generate this)
 *  - Pros: immediate compilation, minimal dependencies, clear dataflow
 *  - Cons: no compile-time graph validation (caught at runtime if wiring is wrong)
 *
 * **Usage**:
 *  ```
 *  val container = AppContainer()
 *  val pipeline = container.analysisPipeline
 *  ```
 *
 * **For tests**: Create a test version of this class with mock implementations:
 *  ```
 *  class TestAppContainer : AppContainer() {
 *      override val analysisPipeline = AnalysisPipeline(
 *          importer = FakeApkImporter(),
 *          // ...
 *      )
 *  }
 *  ```
 */
class AppContainer {
    /** Provides main/io/default coroutine dispatchers for multithreading. */
    val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

    /** Streams APK files from content:// URIs to private app storage with incremental SHA-256 hashing. */
    val apkImporter: ApkImporter = DefaultApkImporter(Sha256HashCalculator)

    /** Validates ZIP structure and builds archive inventory (DEX/native/resource categorization). */
    val apkValidator: ApkValidator = DefaultApkValidator()

    /** Evaluates all rules against analysis facts and produces findings. */
    val findingsEngine: FindingsEngine = DefaultFindingsEngine()

    /**
     * Orchestrates the end-to-end analysis pipeline: import → validate → parse manifest →
     * cross-check → evaluate rules. Wired here with all dependencies.
     */
    val analysisPipeline =
        AnalysisPipeline(
            importer = apkImporter,
            validator = apkValidator,
            findingsEngine = findingsEngine,
            dispatcherProvider = dispatcherProvider,
            clock = SystemClock,
        )
}
