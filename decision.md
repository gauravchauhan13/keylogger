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

