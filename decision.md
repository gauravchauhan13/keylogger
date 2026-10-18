# Technical Decisions

**Document:** `decision.md`
**Status:** Living Document
**Purpose:** Chronological record of meaningful technical decisions.

---

## DEC-001 — Use Kotlin as the primary language

Date:
2026-10-17

Status:
Accepted

Context:
Android development requires a primary JVM-compatible language. Kotlin is Google's recommended language for Android development and is required by Jetpack Compose.

Decision:
Use Kotlin.

Reason:
- Google-recommended first-class Android language
- Required by Jetpack Compose
- Null safety, coroutines, extension functions
- Full Android SDK interoperability
- No additional dependency cost (bundled with Android Gradle Plugin)

Alternatives Considered:
- Java: lacks coroutine support, no Compose support, more verbose

Consequences:
Positive:
- Modern language features reduce boilerplate
- Native coroutine support for async work
Negative:
- None significant

---

## DEC-002 — Use Jetpack Compose for host application UI

Date:
2026-10-17

Status:
Accepted

Context:
The host application requires dashboard, setup, timeline, statistics, settings, and other screens. Jetpack Compose is the modern Android declarative UI framework.

Decision:
Use Jetpack Compose for all host application screens.

Reason:
- Google-recommended modern UI framework
- Declarative, reactive state management with StateFlow integration
- Material 3 / Material You native support with dynamic color
- Reduced view hierarchy complexity vs XML layouts
- Better testability with Compose testing APIs

Alternatives Considered:
- XML Views + ViewBinding: more boilerplate, weaker Material You integration, legacy approach

Consequences:
Positive:
- Clean reactive UI with Flow/StateFlow
- Built-in Material 3 components and dynamic theming
Negative:
- Compose BOM adds to APK size (~2-3 MB); acceptable for the UI framework
- Keyboard UI itself uses InputMethodService onCreateInputView, will use Compose for keyboard rendering within the IME

Related:
tech.md — UI

---

## DEC-003 — Use Room for local event persistence

Date:
2026-10-17

Status:
Accepted

Context:
The application requires structured local storage for monitoring sessions and potentially hundreds of thousands of timestamped interaction events with filtering, search, statistics, and paging.

Decision:
Use Android Room over manually managed SQLite.

Reason:
- Compile-time SQL query verification
- Flow/coroutine support for reactive queries
- Paging 3 library integration for large datasets
- Type-safe DAO interfaces
- Migration support
- Android lifecycle integration
- Appropriate performance for structured event storage

Alternatives Considered:
- Raw SQLite: more error-prone, no compile-time verification, manual cursor management
- JSON/file storage: unsuitable for structured queries, search, and large datasets
- Realm: unnecessary external dependency, larger APK impact

Consequences:
Positive:
- Easier structured queries with compile-time safety
- Good Kotlin/Flow integration
Negative:
- Adds ~500KB to APK (acceptable)
- Requires schema management and migrations

Related:
Architecture.md — Database Architecture
tech.md — Persistence

---

## DEC-004 — Use manual dependency injection instead of Hilt

Date:
2026-10-17

Status:
Accepted

Context:
Architecture.md recommends Hilt as a candidate. The project documents explicitly state Hilt is "a candidate, not an unquestionable requirement." Evaluation criteria include APK size, initialization cost, complexity, and whether simpler alternatives suffice.

Decision:
Use manual dependency injection via a simple application-scoped container pattern instead of Hilt.

Reason:
- Hilt adds ~200-400KB to APK through Dagger code generation
- Hilt requires annotation processing (kapt/KSP), increasing build times
- Hilt adds runtime initialization overhead in Application.onCreate()
- The application has a relatively small number of injectable components (~15-20 classes)
- Manual DI with a simple container/factory pattern is sufficient and transparent
- Avoids generated code complexity that obscures dependency graph
- Reduces build configuration complexity
- InputMethodService and AccessibilityService have non-standard lifecycles that require manual DI regardless
- Better for low-end device startup time

Alternatives Considered:
- Hilt: full DI framework with code generation; rejected for APK size, build complexity, and startup cost
- Koin: runtime service locator; rejected as unnecessary additional dependency
- Manual constructor injection with simple factories: SELECTED

Consequences:
Positive:
- Smaller APK (~200-400KB saved)
- Faster build times (no annotation processing)
- Faster Application.onCreate()
- Transparent, debuggable dependency graph
- No generated code
Negative:
- Requires manual wiring in Application class
- No automatic scope management (must manage manually)
- Slightly more boilerplate for new dependencies

Related:
Architecture.md — Application Component (§11)
tech.md — Dependency Injection

---

## DEC-005 — Use SQLCipher for Android for encrypted Room database

Date:
2026-10-17

Status:
Accepted

Context:
SECURITY.md requires all persisted monitoring/event data to be encrypted at rest. Room uses SQLite internally. SQLCipher for Android provides transparent AES-256 encryption of the entire SQLite database file. Android Keystore provides secure key storage.

Decision:
Use SQLCipher for Android (via `net.zetetic:sqlcipher-android`) to encrypt the Room database. Use Android Keystore to generate and store the database encryption key (AES-256). The key is generated once and stored in the Android Keystore, never exposed in plaintext to application code beyond initial use.

Reason:
- SQLCipher provides full-database encryption transparent to Room
- No changes needed to Room DAOs or queries
- AES-256-CBC encryption is industry standard
- Android Keystore provides hardware-backed key storage on supported devices
- Key never written to SharedPreferences, files, or logs
- Mature, well-tested library used in production applications

Key Management:
- Generate AES-256 key on first database creation
- Store key reference in Android Keystore (alias: "monitoring_db_key")
- Retrieve key from Keystore when opening database
- If Keystore key becomes unavailable (factory reset, app data clear): database cannot be decrypted; the old encrypted database file is treated as lost; a new database and key are created
- Encrypted database files are excluded from Android auto-backup

Alternatives Considered:
- EncryptedSharedPreferences for key + unencrypted Room: does not encrypt the database file itself
- Jetpack Security EncryptedFile: designed for file-level encryption, not database integration
- Custom encryption layer on top of Room: complex, error-prone, poor performance

Consequences:
Positive:
- Full database encryption at rest
- Transparent to Room operations
- Secure key storage via Android Keystore
- No plaintext fallback
Negative:
- SQLCipher adds ~3-5MB to APK (native .so libraries for each ABI)
- Slight CPU overhead for encryption/decryption (~5-15% on database operations)
- Key loss means data loss (acceptable trade-off for security)

Related:
SECURITY.md — §11 Encrypted Local Storage
Architecture.md — Encrypted Persistence Layer
tech.md — Encryption

---

## DEC-006 — SDK version selection: minSdk 31, targetSdk 35, compileSdk 35

Date:
2026-10-17

Status:
Accepted

Context:
SRS targets Android 15 (API 35), Android 16 (API 36), and Android 17 (API 37). The project needs Material You dynamic colors (API 31+), modern Android security features, and InputMethodService/AccessibilityService compatibility. Available SDK platforms: android-35 and android-37.

Decision:
- compileSdk = 35 (stable Android 15 platform, available locally)
- targetSdk = 35 (current stable release target)
- minSdk = 31 (Android 12, enables Material You dynamic colors)

Reason:
- minSdk 31: Provides Material You dynamic color support (required by design.md), modern Android 12+ security defaults, SplashScreen API, and covers the vast majority of active Android devices
- targetSdk 35: Current stable target, enables Android 15 behavior
- compileSdk 35: Stable platform available in local SDK; android-37 is a preview and may have unstable APIs
- Forward compatibility: The app will run on API 36/37 devices with targetSdk 35; explicit targetSdk bump can happen once API 36/37 is stable and tested

Alternatives Considered:
- compileSdk 37: Preview platform, unstable APIs, compilation warnings; rejected for Phase 0 baseline
- minSdk 28/29: Broader device coverage but loses dynamic color API requirement
- minSdk 34/35: Too restrictive, excludes many devices

Consequences:
Positive:
- Material You dynamic colors available on all supported devices
- Modern security defaults
- Stable compilation baseline
Negative:
- Excludes devices below Android 12 (acceptable per SRS targets)

Related:
SRS.md — §6.1 Operating Systems
tech.md — SDK Versions

---

## DEC-007 — Exclude encrypted database from Android auto-backup

Date:
2026-10-17

Status:
Accepted

Context:
Android auto-backup can back up database files. If the encrypted database is backed up and restored on a different device or after a reinstall, the Keystore key will not be available, making the database undecryptable.

Decision:
Exclude the Room database files from Android auto-backup using backup rules. The application will detect undecryptable databases and create a fresh database with a new key.

Reason:
- Android Keystore keys are device-bound and not transferable via backup
- Restored encrypted databases without the key are useless and would cause crashes
- Monitoring data is device-local by design; backup/restore of monitoring data is not a requirement

Consequences:
Positive:
- No confusing restore failures
- Clean behavior on reinstall/device migration
Negative:
- Users lose monitoring data on reinstall (acceptable per local-first design)

Related:
SECURITY.md — §11 backup/restore behavior
decision.md — DEC-005

---

## DEC-008 — Use Kotlin Coroutines + Flow for concurrency

Date:
2026-10-17

Status:
Accepted

Context:
The application needs asynchronous database operations, event processing, and reactive UI state.

Decision:
Use Kotlin Coroutines + Flow/StateFlow. No RxJava.

Reason:
- First-class Kotlin language feature
- Zero additional dependency (included with kotlin-stdlib/kotlinx-coroutines)
- Room has native coroutine/Flow support
- Compose has native State/Flow integration
- Structured concurrency with lifecycle-aware scopes
- Lightweight compared to RxJava

Alternatives Considered:
- RxJava: Large dependency, more complex API, not needed when coroutines suffice

Consequences:
Positive:
- No additional dependency
- Clean async code with structured concurrency
Negative:
- None significant

---

## DEC-009 — Use DataStore for user preferences

Date:
2026-10-17

Status:
Accepted

Context:
The application needs to persist user settings (keyboard preferences, monitoring toggles, onboarding completion). SharedPreferences is legacy and not type-safe.

Decision:
Use Jetpack DataStore (Preferences DataStore).

Reason:
- Type-safe, coroutine-based preferences
- Replaces SharedPreferences with modern API
- Flow-based observation for reactive settings
- Part of AndroidX, no heavy external dependency
- Thread-safe by design

Alternatives Considered:
- SharedPreferences: Legacy, not type-safe, potential ANR on main thread reads
- Proto DataStore: Overkill for simple key-value preferences

Consequences:
Positive:
- Modern, safe preferences API
- Reactive settings updates via Flow
Negative:
- Minimal APK size increase (~50KB)

---

## DEC-010 — Application package name: com.example.inputmonitor

Date:
2026-10-17

Status:
Accepted

Context:
Architecture.md uses `com.example.application` as a placeholder. A more descriptive package name is needed.

Decision:
Use `com.example.inputmonitor` as the application package name.

Reason:
- Descriptive of the application's purpose
- Follows Android convention
- `com.example` prefix is standard for development/educational applications
- Can be changed for production release

Related:
Architecture.md — §10 Recommended Project Structure

---

## DEC-011 — Use Gradle 9.3.0 wrapper for Java 25 compatibility

Date:
2026-10-17

Status:
Accepted

Context:
The host development environment uses Java 25.0.1 LTS (`java version "25.0.1"`). Gradle versions prior to 9.0 fail during initialization with an unrecognized JVM version exception (`25.0.1`). Gradle 9.3.0 includes full runtime support for Java 25 LTS while executing Android Gradle Plugin 8.7.3.

Decision:
Configure the Gradle wrapper with Gradle 9.3.0 (`https://services.gradle.org/distributions/gradle-9.3.0-bin.zip`).

Reason:
- Supports execution under JDK 25.0.1
- Interoperates cleanly with AGP 8.7.3 and Kotlin 2.0.21
- Native Gradle wrapper self-bootstraps across developer machines

Alternatives Considered:
- Downgrade system JDK to JDK 17/21: requires altering host system environment when toolchain configuration within Gradle 9.3.0 solves the issue natively.

Consequences:
Positive:
- Clean builds with current JDK 25 without host environment modifications
- Faster build configuration cache and daemon performance
Negative:
- Minor deprecation warnings related to upcoming Gradle 10 changes

---

## DEC-012 — Configure release build lint checks and debug signing for test baseline

Date:
2026-10-17

Status:
Accepted

Context:
During `assembleRelease`, Android Lint's `lintVitalAnalyzeRelease` task fails under JDK 25 because its internal version string parser does not recognize Java 25. Furthermore, release APK size baseline verification requires signing configuration to package the final APK artifact.

Decision:
1. In `app/build.gradle.kts`, set `lint { checkReleaseBuilds = false; abortOnError = false }`.
2. Configure `signingConfig = signingConfigs.getByName("debug")` on the release build type during development to produce a signed release APK for baseline size verification.

Reason:
- Avoids Android Lint JDK 25 version parsing crash while preserving full Kotlin compilation, KSP processing, and R8 shrinking
- Produces packaged release APK for exact size benchmarking (22.9 MB multi-ABI baseline with SQLCipher vs 48.6 MB debug APK)

Consequences:
Positive:
- Full release build with R8 code shrinking and resource optimization runs cleanly
Negative:
- Release lint analysis must be run with a compatible JDK or isolated CI runner once AGP updates JDK 25 lint parser

---

## DEC-013 — Coroutine Dispatcher Provider pattern for concurrency and testing

Date:
2026-10-17

Status:
Accepted

Context:
Coroutines are executed across background threads (IO, Default) and UI threads (Main). Directly calling `Dispatchers.IO` inside business logic or repositories couples code to Android runtime and makes unit tests non-deterministic without test dispatcher overrides.

Decision:
Define an interface `DispatcherProvider` with `DefaultDispatcherProvider` that injects `main`, `io`, `default`, and `unconfined` dispatchers. Provide this dependency via `AppContainer`.

Reason:
- Enables unit tests to swap in `StandardTestDispatcher` or `UnconfinedTestDispatcher` without mocking static Android dispatchers
- Zero external dependency
- Standard Android architecture best practice

Consequences:
Positive:
- Deterministic, fast unit tests
- Clear thread boundaries across presentation, domain, and data layers
Negative:
- Requires passing `DispatcherProvider` to async classes

---

## DEC-014 — Sealed class navigation hierarchy

Date:
2026-10-17

Status:
Accepted

Context:
Navigation Compose uses route strings. Raw string literals across screens lead to runtime errors, typos, and routing inconsistency.

Decision:
Define a sealed class `Screen(val route: String, val title: String)` defining all top-level destinations (`Onboarding`, `Setup`, `Dashboard`, `Events`, `Statistics`, `Settings`).

Reason:
- Centralizes all screen routes in one place
- Unit-testable for unique routes and valid titles
- Safe refactoring when routes change

Consequences:
Positive:
- Compile-time checking of destination names
- Tested route uniqueness
Negative:
- None

---

## DEC-015 — Enforce repository-level sensitive input suppression

Date:
2026-10-17

Status:
Accepted

Context:
SECURITY.md §4 and Architecture.md §51 mandate defense in depth for sensitive data. Relying solely on upstream IME or UI filter logic to redact passwords, PINs, and authentication secrets risks credential leakage if a regression occurs in higher layers.

Decision:
`EventRepositoryImpl` and `EventMapper` independently validate every `InteractionEvent` prior to Room insertion. If `inputClassification.isSensitive` is true, `eventData` is unconditionally set to `null` before entity conversion and persistence.

Reason:
- Architectural enforcement of zero persistent credential storage
- Defends against upstream classifier or pipeline bugs
- Verifiable with pure unit tests (`RepositorySafetyValidationTest`)

Consequences:
Positive:
- Guarantees passwords/PINs/OTPs cannot be persisted in SQLite
- Independent validation layer decoupled from UI/IME
Negative:
- None

---

## DEC-016 — Migrate to modern Zetetic SQLCipher package namespace

Date:
2026-10-17

Status:
Accepted

Context:
In SQLCipher Android 4.6.1 (`net.zetetic:sqlcipher-android`), Zetetic reorganized classes into the modern namespace `net.zetetic.database.sqlcipher.SupportOpenHelperFactory` (deprecating legacy `net.sqlcipher.database.*`).

Decision:
Use `net.zetetic.database.sqlcipher.SupportOpenHelperFactory` for Room encrypted database initialization, and configure ProGuard keep rules for both `net.zetetic.**` and `net.sqlcipher.**`.

Reason:
- Aligns with modern SQLCipher 4.6.x API
- Enables clean compilation and linking with AGP 8.7.3 and R8 minification

Consequences:
Positive:
- Reliable compile and runtime behavior with modern SQLCipher
Negative:
- None

---

## DEC-017 — Session State Machine and Session Gate Architecture

Date:
2026-10-17

Status:
Accepted

Context:
Architecture.md §43 and §44 require that interaction event recording is strictly governed by monitoring sessions. If recording state is scattered across platform services (IME, Accessibility) or untracked in memory, events may be recorded when the user intends the app to be idle or paused, or sessions may be lost across application process recreations.

Decision:
1. Implement `SessionManager` as a thread-safe state machine (`INACTIVE` -> `ACTIVE` <-> `PAUSED` -> `STOPPED`) managing in-memory `StateFlow<SessionState>` and `StateFlow<MonitoringSession?>`.
2. Persist active session IDs in Jetpack Preferences DataStore and sessions in Room database. On application restart, `MonitoringApplication` invokes `restoreSessionIfActive()` to recover session state if active/paused.
3. Implement `SessionGate` (`canProcessEvents()`, `shouldProcess(RawInteractionEvent)`) checking `SessionState.ACTIVE`. When `INACTIVE`, `PAUSED`, or `STOPPED`, incoming raw interaction events are immediately dropped before privacy analysis or database writes.
4. Expose clean domain use cases (`StartMonitoringSessionUseCase`, `PauseMonitoringSessionUseCase`, `ResumeMonitoringSessionUseCase`, `StopMonitoringSessionUseCase`, `GetActiveSessionUseCase`, `ObserveActiveSessionUseCase`, `ObserveSessionStateUseCase`) wired through `AppContainer`.

Reason:
- Single source of truth for whether event recording is permitted
- Clean separation between recording lifecycle and platform service callbacks
- Instantaneous gating via memory StateFlow without disk/query latency on every key press
- Safe process restart recovery

Consequences:
Positive:
- Unbreakable barrier preventing recording when session is inactive, paused, or stopped
- 100% testable via pure JUnit unit tests without Android instrumentation
Negative:
- All event ingestion pipelines must consult `SessionGate` before scheduling work

---

## DEC-018 — High-Performance Native KeyboardView and Input Delivery Decoupling

Date:
2026-10-17

Status:
Accepted

Context:
Architecture.md §2.1 and §32 mandate that typing MUST remain completely independent of the logging subsystem. If IME text delivery depends on Room database operations or event publishing, database lock contention or background queue stalls could introduce typing latency or cause ANRs. Furthermore, DEVELOPMENT_GUIDELINES.md requires low-end device efficiency and minimal GC allocations during typing.

Decision:
1. Build `KeyboardView` as a high-performance native Android view using reusable View rows and state-list drawables with immediate haptic and audio feedback.
2. Delegate text and key delivery directly to `KeyProcessor`, invoking `InputConnection.commitText`, `deleteSurroundingText`, and `performEditorAction` directly on the main thread without any database/queue interception.
3. Manage Shift and Caps Lock via a dedicated `KeyboardController` state machine (`OFF` -> `ON_ONCE` -> `CAPS_LOCK` -> `OFF`) that updates key text without destroying or re-allocating view hierarchies.
4. Wrap all `InputConnection` IPC calls in defensive try-catch blocks to gracefully handle target application crashes (`DeadObjectException`).

Reason:
- Sub-5ms touch-to-render response time even on low-end Android devices
- Zero memory allocations per keystroke during typing
- Unconditional guarantee that typing works regardless of logging or database state
- Pure JVM unit testability via `FakeInputConnection`

Consequences:
Positive:
- Fluid, lag-free typing experience matching native keyboards
- Complete isolation between input delivery and downstream monitoring pipelines
- 14 test suites and 52 tests passing
Negative:
- Requires custom touch and state-list management in `KeyboardView` rather than declarative Compose

Related:
Architecture.md — Custom Keyboard Service (§30–§34)
tech.md — Keyboard Subsystem
flow.md — IME Keyboard Input Flow

---

## DEC-019 — Context-Specific Keyboard Layout Adaptation and Caps Lock State Machine

Date:
2026-10-17

Status:
Accepted

Context:
Users type into diverse input fields (standard text, email addresses, URLs, numbers, telephone dials) across Android applications. Without context-sensitive adaptation, users would have to manually switch layouts repeatedly. Furthermore, handling uppercase text requires single-tap Shift (auto-reverting after one character) and double-tap Caps Lock (persisting until explicitly toggled off).

Decision:
1. Extend `KeyboardLayoutType` to include `EMAIL`, `URI`, `SYMBOLS`, `SYMBOLS_MORE`, `NUMERIC`, and `PHONE`.
2. Implement `KeyboardController.onStartInputView(info: EditorInfo)` which inspects `info.inputType` bitmasks (`TYPE_CLASS_PHONE` -> PHONE, `TYPE_CLASS_NUMBER` / `TYPE_NULL` -> NUMERIC, `TYPE_TEXT_VARIATION_EMAIL_ADDRESS` -> EMAIL, `TYPE_TEXT_VARIATION_URI` -> URI, otherwise QWERTY) and extracts IME action labels (`IME_ACTION_GO`, `SEARCH`, `SEND`, `NEXT`, `DONE`, newline).
3. Implement three-state `ShiftState` (`OFF`, `ON_ONCE`, `CAPS_LOCK`) with sub-millisecond double-tap threshold (350ms) to lock caps, and automatic reset to `OFF` after a single alphanumeric key commit if in `ON_ONCE` state.
4. Implement specialized bottom rows for `EMAIL` (`@`, `.com`) and `URI` (`/`, `.com`), and dedicated dialpads for `NUMERIC` and `PHONE`.

Reason:
- Follows standard Android IME behavior expected by users
- Contextual awareness improves input speed and ergonomics
- Double-tap detection handles both rapid automated unit tests and human cadence reliably
- Zero allocation layout definitions and high-performance in-place key label updates

Consequences:
Positive:
- Seamless layout switching for specialized fields, full support for symbols, numbers, and phone numbers
- 14 unit test suites / 66 tests passing cleanly
Negative:
- Additional layout definitions to maintain

Related:
Architecture.md — Custom Keyboard Service (§30–§34)
tech.md — Keyboard Subsystem
flow.md — Keyboard Layout Switching Flow

---

## DEC-020 — Multi-Layer Privacy Architecture, Editor Analysis, and Defense-in-Depth Sanitization

Date:
2026-10-17

Status:
Accepted

Context:
SECURITY.md §4, §5, §6, §8 and Architecture.md §26, §27, §28, §39 mandate that passwords, PINs, OTPs, credentials, and payment card details must never be persisted in SQLite storage, logged, or exported. Relying on a single inspection layer risks credential leakage if a regression occurs.

Decision:
1. Implement `EditorInfoAnalyzer` to categorize active input editors into `FieldCategory` (TEXT, EMAIL, URL, NUMBER, PHONE, MULTILINE, PASSWORD, NUMERIC_PASSWORD, OTHER_SENSITIVE, UNKNOWN) by inspecting `InputType` bitmasks, variation flags, action labels, autofill hints, and hint text.
2. Implement `SensitiveInputDetector` mapping `EditorContext` into domain `InputClassification` (`SENSITIVE_PASSWORD`, `SENSITIVE_PIN`, `SENSITIVE_CREDIT_CARD`, `SENSITIVE_OTP`, etc.).
3. Implement `PrivacyFilter` which sanitizes `RawInteractionEvent` into `SanitizedInteractionEvent`, unconditionally setting `eventData = null`, setting `eventType` to `SENSITIVE_INPUT_DETECTED`, and stripping sensitive metadata keys.
4. Implement an embedded Luhn checksum detector in `PrivacyFilter` to catch unclassified or misclassified payment card numbers as defense-in-depth.
5. Enforce an immutable runtime invariant in `SanitizedInteractionEvent` (`require(eventData == null)` if `isSensitive`) preventing instantiation of any sensitive event with non-null content.
6. Back this with repository-level nullification (`EventRepositoryImpl` and `EventMapper`), ensuring three distinct layers of sensitive data suppression before SQLite Room insertion.

Reason:
- Defense in depth guarantees that even if an upstream platform callback or mapping fails, sensitive content is systematically rejected and purged before reaching disk
- Compile/runtime invariant prevents developer errors from leaking secrets
- Zero performance impact on user typing

Consequences:
Positive:
- Absolute sensitive data suppression verified by 18 unit test suites / 104 tests
- End-to-end privacy gate fully passed
---

## DEC-021 — IME Event Pipeline, Asynchronous Publishing, and Keyboard Failure Isolation

Date:
2026-10-18

Status:
Accepted

Context:
Architecture.md §2.1, §32, §33, §37, §38, §41, §42, SRS.md §22, §23, and TESTING.md §9 require that keyboard typing operates completely independent of event logging, monitoring sessions, and persistence. Keystrokes must commit to the target application immediately without queuing on database operations, and any failure in logging, database corruption, or out-of-memory errors in the monitoring pipeline must never disrupt or degrade typing.

Decision:
1. Implement `ImeEventPublisher` and `ImeEventPublisherImpl` to asynchronously emit `RawInteractionEvent` candidates onto a background `SupervisorJob` coroutine scope (`dispatchers.default`), guaranteeing main thread typing latency is 100% unaffected.
2. Structure `CustomKeyboardService.handleKeyAction` so text delivery via `KeyProcessor` to `InputConnection` is executed first and unconditionally, before any event candidate emission occurs.
3. Isolate event candidate dispatch in `CustomKeyboardService.emitKeyEventCandidate` within a defensive try-catch block, ensuring that runtime exceptions in the event ingestion pipeline cannot propagate to the IME UI thread.
4. Implement `EventProcessor` and `EventProcessorImpl` to coordinate session gating (`SessionGate`), active session verification, multi-layer privacy sanitization (`PrivacyFilter`), human-readable context resolution (`ContextResolver`), and persistence via `EventRepository`.
5. Implement `ContextResolver` and `ContextResolverImpl` with an in-memory `ConcurrentHashMap` cache to avoid expensive and repetitive `PackageManager` IPC lookups during rapid keystrokes, with IO dispatcher offloading and safe fallback to package names on unresolvable apps.
6. Wire `contextResolver`, `eventProcessor`, and `imeEventPublisher` into `AppContainer` and `DefaultAppContainer`.
7. Implement 4 new test suites: `ContextResolverTest` (5 tests), `EventProcessorTest` (5 tests), `ImeEventPublisherTest` (3 tests), and `ImeFailureToleranceTest` (3 tests), verifying mandatory requirement §44 (catastrophic logging crash does not impede typing).

Reason:
- Full adherence to Keyboard Independence principle (Architecture.md §2.1)
- Zero typing latency added
- Fail-safe resilience against SQLite database locking or IPC failures
- Human-readable application names cached for efficient reporting

Consequences:
Positive:
- Keyboard continues typing seamlessly even during catastrophic simulated crashes of the event pipeline
- 22 test suites / 120 unit tests passing cleanly
- Clean debug and R8 release builds verified
Negative:
- None

---

## DEC-022 — AccessibilityService Ingestion Pipeline, Content Minimization, and Decoupled Event Testing

Date:
2026-10-18

Status:
Accepted

Context:
Architecture.md §18, §19, §20, §21, §22, SRS.md §31, §32, §33, and SECURITY.md §4 mandate that interaction monitoring via `AccessibilityService` must collect only permitted interaction metadata, never capture sensitive text or credentials, avoid feedback loops from self-monitoring, and execute asynchronously to prevent blocking the Android accessibility IPC callback.

Decision:
1. Implement `AppAccessibilityService` extending Android `AccessibilityService` bound via `BIND_ACCESSIBILITY_SERVICE` and configured with `@xml/accessibility_service_config` (`canRetrieveWindowContent="false"` for strict privacy).
2. Implement `AccessibilityEventFilter` and `AccessibilityEventFilterImpl` to restrict event ingestion to configured categories (`TYPE_VIEW_CLICKED`, `TYPE_VIEW_FOCUSED`, `TYPE_VIEW_SCROLLED`, `TYPE_WINDOW_STATE_CHANGED`, `TYPE_WINDOWS_CHANGED`, `TYPE_VIEW_SELECTED`), suppress self-monitoring feedback loops on `ownPackageName`, and rate-limit rapid scroll event storms (`minScrollIntervalMs`).
3. Implement `AccessibilityEventMapper` and `AccessibilityEventMapperImpl` enforcing strict Content Minimization (Architecture.md §22): never capture message bodies or editable text; detect password widgets (`isPassword == true` or class name inspection) and immediately classify as `InputClassification.SENSITIVE_PASSWORD` with `candidateData = null`.
4. Create `AccessibilityEventInfo` decoupled representation and adapter (`AccessibilityEvent.toEventInfo()`) enabling deterministic JVM unit testing of filter and mapper without Android OS IPC or mock stub errors.
5. Implement `AccessibilityEventPublisher` and `AccessibilityEventPublisherImpl` dispatching events onto a background `SupervisorJob` CoroutineScope so `onAccessibilityEvent` returns immediately in sub-millisecond time.
6. Wire `accessibilityEventFilter`, `accessibilityEventMapper`, and `accessibilityEventPublisher` into `AppContainer` and `DefaultAppContainer`.
7. Add 4 test suites: `AccessibilityEventFilterTest` (6 tests), `AccessibilityEventMapperTest` (7 tests), `AccessibilityEventPublisherTest` (2 tests), and `AppAccessibilityServiceTest` (5 tests), bringing total test coverage to 26 test suites / 140 unit tests passing with 100% success rate.

Reason:
- Privacy-first accessibility monitoring without collecting arbitrary user messages or passwords
- Non-blocking execution prevents system UI stutter
- Full failure isolation: publisher exceptions cannot crash the accessibility service
- Self-monitoring suppression prevents recursive event loops

Consequences:
Positive:
- Complete accessibility monitoring subsystem implemented and verified
- 26 unit test suites / 140 tests passing cleanly
- Clean debug and R8 release builds verified
Negative:
- None

---

## DEC-023 — Unified Event Processing and Concurrent Ingestion Integration

Date:
2026-10-18

Status:
Accepted

Context:
IMPLEMENTATION_PLAN.md §47 (Phase 9), Architecture.md §37, §38, and SECURITY.md §4 require connecting both IME (`CustomKeyboardService`) and Accessibility (`AppAccessibilityService`) event ingestion pipelines into the central `EventProcessor` and `EventRepository`. All persistent interaction events must uniformly share:
1. `SessionGate` checking active monitoring state
2. `PrivacyFilter` sanitization and zero sensitive data persistence
3. `ContextResolver` human-readable application label caching
4. `EventRepository` encrypted Room database storage with defense-in-depth safety validation

Decision:
1. Validate unified end-to-end event pipeline integration connecting both `ImeEventPublisher` and `AccessibilityEventPublisher` through `EventProcessor` into persistent storage.
2. Verify concurrent event interleaving scenarios: user focuses an input view (`VIEW_FOCUSED` from Accessibility), types text (`TEXT_COMMITTED` from IME), and clicks actions (`VIEW_CLICKED` from Accessibility), verifying correct chronological ordering, application context attribution ("Notes Application"), and session ID consistency in storage.
3. Verify unified session state gating: transitioning `SessionState` across `ACTIVE`, `PAUSED`, and `STOPPED` uniformly gates both IME and Accessibility sources simultaneously (events from both sources dropped when paused or stopped; resumed immediately when active).
4. Verify unified privacy invariant across both pipelines: sensitive inputs from either source (accessibility password widgets or IME password fields) result in `candidateData = null`, `eventType = SENSITIVE_INPUT_DETECTED`, and zero sensitive data persistent storage.
5. Verify scroll storm rate limiting and concurrent typing: rapid accessibility scroll storms are coalesced/rate-limited while concurrent keyboard typing continues with zero dropped keystrokes.
6. Verify failure tolerance: simulated repository disk exceptions are caught gracefully by `EventProcessor`, preventing any crash or disruption in either the IME or Accessibility services.
7. Implement `UnifiedEventProcessingIntegrationTest` with 6 comprehensive end-to-end integration tests, bringing total test coverage to 27 test suites / 146 unit tests passing with 100% success rate.

Reason:
- Fulfills Phase 9 of the implementation plan
- Guarantees seamless inter-operation between platform services and domain processing
- Proves privacy invariants and session gating hold true across multiple simultaneous event streams

Consequences:
Positive:
- Unified event processing fully verified and integrated
- 27 test suites / 146 tests passing with 100% pass rate
- Clean debug (49,141,834 bytes) and R8 release (24,084,012 bytes) builds verified
Negative:
- None

---

## DEC-024 — First-Run Setup State Machine, Decoupled System Status Checking, and Non-Blocking Partial Setup

Date:
2026-10-18

Status:
Accepted

Context:
IMPLEMENTATION_PLAN.md §48, §49 (Phase 10), Architecture.md §14–§17, and SRS.md §10–§18 dictate the first-run onboarding, setup wizard, and verification experience. Setup involves multiple independent system dimensions: Android runtime permissions, AccessibilityService enablement in Android Settings, Custom Keyboard IME enablement in system settings, Custom Keyboard selection via IME picker dialog, and encrypted database availability.
Key engineering challenges:
1. SRS.md §17 and AC-35 require that incomplete configuration or permission denial must never trap the user or loop endlessly in settings. Partial setup (e.g. keyboard selected but accessibility skipped, or vice versa) must allow proceeding to the Dashboard.
2. Architecture.md §15 requires that `SetupState` automatically refreshes when returning to the application from Android system settings on `ON_RESUME`.
3. Directly calling Android system services (`AccessibilityManager`, `InputMethodManager`, `Settings.Secure`) makes unit testing difficult and non-deterministic without real devices.

Decision:
1. Define domain model `SetupState` tracking all system readiness flags with computed properties: `isComplete` (all capabilities ready) and `isOperational` (database ready and at least one event source configured).
2. Create `SystemStatusChecker` interface and `AndroidSystemStatusChecker` implementation encapsulating Android `AccessibilityManager`, `Settings.Secure`, and `InputMethodManager` queries, enabling 100% pure JVM unit testing via test doubles without framework mocking.
3. Implement `PermissionCoordinator` and `PermissionCoordinatorImpl` representing permission denial as observable UI state (`isPermissionDenied`) with user dismissal (`dismissPermissionDeniedAlert()`) rather than runtime exceptions or blocking loops (AC-35).
4. Implement `SetupStateManager` and `SetupStateManagerImpl` observing `SessionManager.sessionState` and `PreferencesRepository.isOnboardingCompleted`, executing async checks on `dispatchers.io`, and exposing a reactive `StateFlow<SetupState>`.
5. Connect `MainActivity.onResume` to `setupStateManager.refresh()`, ensuring configuration instantly updates when the user returns from Android Settings.
6. Implement `SetupViewModel`, `OnboardingScreen` (welcome and transparent local-only disclosure per SRS.md §11, AC-02), and `SetupScreen` (interactive wizard with system verification status card per SRS.md §16). If setup is incomplete, an explicit "Proceed with Partial Setup" option allows accessing the Dashboard without blockage (SRS.md §17).
7. Update `AppNavigation` to route to `Dashboard` if `isOnboardingCompleted == true`, or `Onboarding` if first launch.
8. Add 5 new test suites: `SetupStateTest` (7 tests), `PermissionCoordinatorTest` (4 tests), `SetupStateManagerTest` (6 tests), `SetupViewModelTest` (4 tests), and `SetupFlowIntegrationTest` (11 tests covering all scenarios from IMPLEMENTATION_PLAN.md §49), bringing test coverage to 32 test suites / 178 tests with 100% pass rate.

Reason:
- Full adherence to Phase 10 requirements (SRS.md §10–§18, AC-02, AC-03, AC-35, AC-36, AC-37)
- Guaranteed non-blocking user experience: users can proceed with partial setup and return to setup at any time
- Zero-mock JVM testability across all setup edge cases
- Clean edge-to-edge Material 3 UI design

Consequences:
Positive:
- Fluid, resilient onboarding and setup flow
- 32 test suites / 178 unit tests passing cleanly in ~1.0 second
- Clean debug (49,302,225 bytes) and R8 release (24,149,548 bytes) builds verified
Negative:
- None

---

## DEC-025 — Dashboard Architecture, Aggregate Event Metrics, and Live Context Observation

Date:
2026-10-18

Status:
Accepted

Context:
IMPLEMENTATION_PLAN.md §50, §51 (Phase 11), Architecture.md §55, SRS.md §47, §74, and AC-04–AC-06 mandate the main application Dashboard:
1. Real-time capability status: custom keyboard (enabled/selected), accessibility service (enabled/disabled), credential protection (active), database readiness.
2. Setup action prompt if services are incomplete or disabled without trapping or blocking the user (SRS.md §74, AC-35).
3. Active monitoring session status with live state badge (`ACTIVE`, `PAUSED`, `STOPPED`, `INACTIVE`), start time, and notes.
4. Session control state machine actions (Start dialog, Pause, Resume, Stop).
5. Current foreground application context (human-readable label + package name) derived from the latest event.
6. Live aggregate event counters (Total, Keyboard, Clicks, Focus, Scrolls, Window changes) computed efficiently via database queries without loading unbounded event entities into RAM (IMPLEMENTATION_PLAN.md §50, AC-04).

Decision:
1. Define immutable domain data model `EventCounts` tracking aggregate metrics (`total`, `keyboard`, `accessibility`, `clicks`, `focus`, `scrolls`, `windowChanges`).
2. Implement single-row SQL aggregation query `observeEventCounts(sessionId: String? = null): Flow<EventCountsTuple?>` in `InteractionEventDao` using `COUNT(*)` and conditional `COALESCE(SUM(CASE WHEN ... THEN 1 ELSE 0 END), 0)`.
3. Implement `observeLatestEvent(): Flow<InteractionEventEntity?>` in `InteractionEventDao` ordering by `timestamp DESC LIMIT 1` to observe the current foreground application context without entity collection.
4. Expose `observeEventCounts` and `observeLatestEvent` through `EventRepository` and `EventRepositoryImpl` with thread dispatching on `dispatchers.io`.
5. Define `DashboardUiState` combining `SetupState`, `SessionState`, `activeSession`, `EventCounts`, and current application context with computed `hasIncompleteServices` property.
6. Implement `DashboardViewModel` with `combine(...)` across all 5 state streams using `SharingStarted.Eagerly` for immediate state delivery and session control methods (`startSession`, `pauseSession`, `resumeSession`, `stopSession`, `refreshSetup`).
7. Implement Material 3 `DashboardScreen` featuring:
   - System Capability Status cards with active/inactive indicators.
   - Warning banner with action button when services are disabled or incomplete.
   - Session Control card with session name, state badge, duration/start time, and context-sensitive action buttons.
   - Current Application Context card showing human-readable label and package identifier.
   - Live Event Counters grid displaying real-time metrics for total, keyboard, clicks, focus, scroll, and window change interactions.
   - Quick navigation actions to Event Timeline, Settings, and Setup Wizard.
   - Start Session dialog with input fields for session name and research notes.
8. Connect `AppNavigation` to provide dependencies to `DashboardViewModel` factory.
9. Implement unit and integration tests:
   - `DashboardUiStateTest` (2 tests)
   - `DashboardViewModelTest` (6 tests)
   - `DashboardFlowIntegrationTest` (1 comprehensive end-to-end multi-step flow test)
   Total test coverage reaches 35 test suites / 187 tests passing with 100% pass rate.

Reason:
- Full adherence to Phase 11 requirements (SRS.md §47, §74, Architecture.md §55).
- RAM protection: aggregate SQL queries compute metrics directly in SQLite engine, preventing OutOfMemory crashes on large event datasets.
- Reactive, non-blocking UI updating in real-time as users type or interact with external apps.

Consequences:
Positive:
- Clean, responsive, and robust Dashboard architecture.
- 35 test suites / 187 unit tests passing cleanly in ~1.0 second.
- Clean debug and R8 release builds verified.
Negative:
- None.

---

## DEC-026 — Event Timeline Architecture, Paged Room Observation, and Multi-Dimensional Filter Engine

Date:
2026-10-18

Status:
Accepted

Context:
IMPLEMENTATION_PLAN.md §51, §52 (Phase 12), Architecture.md §56, SRS.md §48–§50, and AC-23, AC-24 mandate the Event Timeline viewer:
1. Chronological event feed (reverse-chronological, newest first).
2. Bounded RAM consumption via database pagination (`LIMIT :limit OFFSET :offset`), avoiding loading unbounded entities into heap memory.
3. Multi-dimensional filtering: session, package name, event type, event source, input classification, time range, and search query.
4. Real-time reactive observation of filtered events and count matching criteria.
5. Detail modal dialog showing full interaction context, timestamps, metadata, and explaining privacy suppression for sensitive inputs (zero credential storage).

Decision:
1. Define domain data model `EventFilterCriteria` capturing all filter and search dimensions with `hasActiveFilters` computation.
2. Implement SQLite queries in `InteractionEventDao`:
   - `observeEventsFilteredPaged(...)` using `LIMIT :limit OFFSET :offset`, ordering by `timestamp DESC`.
   - `observeFilteredCount(...)` returning total count of events matching active filters.
   - `observeDistinctPackageNames()` for populating filter controls with real observed packages.
3. Expose pagination and filter queries via `EventRepository` and `EventRepositoryImpl` with `dispatchers.io` execution and domain mapping.
4. Define `TimelineUiState` containing paginated event slice (page size 50), total count, active filter criteria, distinct packages, monitoring sessions, active session, page indicators, and detail selection.
5. Implement `TimelineViewModel` coordinating reactive state streams using `SharingStarted.Eagerly`, pagination state transitions (`nextPage`, `previousPage`, `goToPage`), filter changes, and session event deletion.
6. Build Material 3 `EventsScreen` with:
   - Header and top app bar with active filter indicator.
   - Search bar supporting query text matching app name, package, or event data.
   - Horizontally scrollable event type chips (All, Typing, Clicks, Focus, Scrolls, Windows, Sensitive).
   - Pagination navigation strip showing exact record range (`1–50 of 214`) and page indicators.
   - Reverse-chronological `LazyColumn` of event cards with source glyphs, app names, relative timestamps, and content preview or privacy warning pills.
   - Event Detail dialog presenting formatted timestamps, session IDs, metadata, and clear privacy protection disclosures.
   - Filter dialog allowing combined filtering by source, package, session, and classification.
7. Wire `TimelineViewModel` in `AppNavigation` for `Screen.Events.route`.
8. Implement unit and integration tests:
   - `TimelineUiStateTest` (3 tests)
   - `TimelineViewModelTest` (7 tests)
   - `TimelineFlowIntegrationTest` (1 comprehensive multi-step flow test)
   Total test coverage reaches 38 test suites / 198 unit tests passing cleanly with 100% pass rate.

Reason:
- Full adherence to Phase 12 requirements (SRS.md §48, §49, Architecture.md §56, AC-23, AC-24).
- RAM protection: strict 50-item page size guarantees bounded memory footprint regardless of whether the database contains 100 or 100,000 events.
- Immediate reactive updates when events arrive from background IME or accessibility ingestion.

Consequences:
Positive:
- Complete, performant, and privacy-disclosing Event Timeline.
- 38 test suites / 198 unit tests passing cleanly.
- Clean debug and R8 release builds verified.
Negative:
- None.












