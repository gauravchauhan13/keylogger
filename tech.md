# Technology Stack

**Document:** `tech.md`
**Status:** Living Document / Authoritative
**Purpose:** Current implemented technology stack. This document is authoritative once created.

---

## Platform

Android

---

## Language

Kotlin

Reason:
Google-recommended first-class Android language. Required by Jetpack Compose. Native coroutine support.

---

## UI

Jetpack Compose (via Compose BOM)

Reason:
Modern declarative UI framework. Native Material 3 / Material You support. Reactive state integration with Flow/StateFlow.

---

## Design System

Material Design 3 (Material You)

Dynamic system colors on Android 12+ (API 31+).

Reason:
Required by design.md. Native Compose integration via Material3 library.

---

## Architecture

Lightweight layered architecture inspired by Clean Architecture + MVVM + Repository Pattern

Layers:
- Presentation (Compose / ViewModels)
- Domain (Models, use cases, privacy rules, session rules)
- Data (Room, repositories, mapping, preferences, export)
- Platform (IME, AccessibilityService, Android integration)

Reason:
Per Architecture.md. Provides meaningful separation without overengineering.

---

## SDK Versions

```text
minSdk:      31  (Android 12)
targetSdk:   35  (Android 15)
compileSdk:  35  (Android 15)
```

Reason:
- minSdk 31 enables Material You dynamic colors and modern Android 12+ APIs
- targetSdk/compileSdk 35 is the current stable platform
- See DEC-006

---

## Database

Room (AndroidX Room)

Reason:
Compile-time SQL verification, Flow support, paging integration, migration support. See DEC-003.

---

## Encryption

SQLCipher for Android (`net.zetetic:sqlcipher-android`)

Key Management:
Android Keystore (AES-256 key, alias: `monitoring_db_key`)

Reason:
Full-database transparent encryption at rest. Secure hardware-backed key storage. See DEC-005.

---

## Concurrency

Kotlin Coroutines + Flow / StateFlow

Reason:
First-class Kotlin feature. Zero additional dependency. Room and Compose native support. See DEC-008.

---

## Configuration / Preferences

Jetpack DataStore (Preferences DataStore)

Reason:
Type-safe, coroutine-based, Flow-based observation. Replaces SharedPreferences. See DEC-009.

---

## Dependency Injection

Manual dependency injection (constructor injection + application-scoped container)

Reason:
Avoids Hilt's APK size (~200-400KB), build-time annotation processing, and startup overhead. Sufficient for ~15-20 injectable components. See DEC-004.

---

## Keyboard

- Framework: Android `InputMethodService` (`CustomKeyboardService`)
- Subtype: English US (`en_US`, keyboard mode)
- Layout Engine: `KeyboardLayout` multi-layout engine: QWERTY, Symbols, More Symbols, Numeric, Phone, Email, and URI (DEC-019)
- Context Adaptation: Automatic layout selection and IME action label resolution based on active `EditorInfo` (DEC-019)
- View Architecture: Native Android `KeyboardView` with state-list drawables, sub-5ms touch response, zero GC allocations per key press
- Key Processor: `KeyProcessor` direct `InputConnection` text and key delivery (`commitText`, `deleteSurroundingText`, `performEditorAction`), completely decoupled from logging and persistence (DEC-018)
- Shift & Caps Lock: `KeyboardController` 3-stage state machine (`OFF` -> `ON_ONCE` -> `CAPS_LOCK` -> `OFF`) with double-tap detection and single-character auto-reset
- Feedback: Android `AudioManager` sound effects (`FX_KEYPRESS_STANDARD`) and `HapticFeedbackConstants.KEYBOARD_TAP`
- Switching: Native `InputMethodManager.showInputMethodPicker()` integration

Reason:
Native Android IME framework. Only supported way to implement a custom keyboard. Decoupled architecture guarantees typing reliability regardless of application, database, or logging state.

---

## Privacy & Sensitive Input Protection

- Editor Analysis: `EditorInfoAnalyzer` / `EditorInfoAnalyzerImpl` inspecting inputType bitmasks, variations, action labels, and hint attributes into `FieldCategory` and `EditorContext` (DEC-020)
- Classification: `SensitiveInputDetector` / `SensitiveInputDetectorImpl` classifying fields into `InputClassification` (Password, PIN, Credit Card, OTP, Normal, Search, Number)
- Sanitization: `PrivacyFilter` / `PrivacyFilterImpl` discarding candidateData, replacing eventType with `SENSITIVE_INPUT_DETECTED`, stripping unsafe metadata, and defense-in-depth Luhn card number detection
- Invariant Safety: `SanitizedInteractionEvent` runtime constructor assertion (`require(eventData == null)` when `isSensitive`)
- Persistence Defense: `EventRepositoryImpl` and `EventMapper` independently nullifying sensitive content (DEC-015, DEC-020)

Reason:
Mandated by SECURITY.md §4, §5, §6, §8 and Architecture.md §26, §27, §28, §39. Multi-layer defense in depth ensures zero persistent credential leakage.

---

## Interaction Monitoring

Android `AccessibilityService`

Reason:
Official Android framework for permitted accessibility-event observation.

---

## Build & Toolchain

- Gradle: 9.3.0 (wrapper)
- Android Gradle Plugin (AGP): 8.7.3
- Kotlin Gradle Plugin & Compose Compiler Plugin: 2.0.21
- KSP: 2.0.21-1.0.28
- Java Runtime: Oracle JDK 25.0.1 LTS
- Bytecode Target: Java 17 (`JavaVersion.VERSION_17`)
- Build Language: Gradle Kotlin DSL (`build.gradle.kts`)

Reason:
Type-safe build configuration. Gradle 9.3.0 provides full runtime compatibility with host Java 25 LTS while executing AGP 8.7.3. See DEC-011.

---

## Testing

```text
JUnit 4                 — Unit tests (junit:junit:4.13.2)
Kotlin Coroutines Test  — Coroutine testing (1.9.0)
Room Testing            — Database tests (2.6.1)
Compose UI Testing      — UI tests (androidx.compose.ui:ui-test-junit4)
Espresso Core           — Instrumentation (3.6.1)
```

---

## Baseline Build Metrics

| Metric | Phase 0 Baseline | Phase 1 Foundation | Phase 2 Data Foundation | Phase 3 Monitoring Sessions | Phase 4 Basic Keyboard | Phase 5 Complete Keyboard | Phase 6 Editor & Privacy | Phase 7 IME Pipeline | Phase 8 Accessibility | Phase 9 Integration | Phase 10 First-Run Setup | Phase 11 Dashboard | Phase 12 Event Timeline | Phase 13 Search Engine | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Debug APK Size | 48,608,524 bytes (~46.35 MB) | 48,720,225 bytes (~46.46 MB) | 48,887,522 bytes (~46.62 MB) | 49,048,518 bytes (~46.78 MB) | 49,127,138 bytes (~46.85 MB) | 49,137,994 bytes (~46.86 MB) | 49,131,050 bytes (~46.85 MB) | 49,141,834 bytes (~46.86 MB) | 49,141,834 bytes (~46.86 MB) | 49,141,834 bytes (~46.86 MB) | 49,302,225 bytes (~47.02 MB) | 49,413,901 bytes (~47.12 MB) | 49,652,761 bytes (~47.35 MB) | 49,542,941 bytes (~47.25 MB) | Multi-ABI, unminified, Room + SQLCipher + DataStore + SessionManager + CustomKeyboardService + PrivacyFilter + EventProcessor + ContextResolver + AppAccessibilityService + SetupStateManager + DashboardScreen + EventsScreen + Debounced Search |
| Release APK Size | 22,945,972 bytes (~21.88 MB) | 23,318,576 bytes (~22.24 MB) | 23,646,256 bytes (~22.55 MB) | 24,067,628 bytes (~22.95 MB) | 24,067,628 bytes (~22.95 MB) | 24,067,628 bytes (~22.95 MB) | 24,084,012 bytes (~22.96 MB) | 24,084,012 bytes (~22.96 MB) | 24,084,012 bytes (~22.96 MB) | 24,084,012 bytes (~22.96 MB) | 24,149,548 bytes (~23.03 MB) | 24,165,932 bytes (~23.05 MB) | 24,297,004 bytes (~23.17 MB) | 24,313,388 bytes (~23.19 MB) | Multi-ABI, R8 minified + shrunk resources. Search indexing + debounced StateFlow query engine + auto-suggestions strip |
| Unit Test Suite | 1 test (PASSED) | 3 suites / 5 tests (PASSED) | 8 suites / 17 tests (PASSED) | 11 suites / 37 tests (PASSED) | 14 suites / 52 tests (PASSED) | 14 suites / 66 tests (PASSED) | 18 suites / 104 tests (PASSED) | 22 suites / 120 tests (PASSED) | 26 suites / 140 tests (PASSED) | 27 suites / 146 tests (PASSED) | 32 suites / 178 tests (PASSED) | 35 suites / 187 tests (PASSED) | 38 suites / 198 tests (PASSED) | 39 suites / 216 tests (PASSED) | Added SearchQueryTest (9), updated EventRepositoryTest (+2), TimelineViewModelTest (+5), TimelineFlowIntegrationTest (+2). 100% pass rate in ~21s |
| Gradle Tasks Executed | 70 tasks | 89 tasks | 91 tasks | 94 tasks | 97 tasks | 97 tasks | 84 tasks | 84 tasks | 84 tasks | 84 tasks | 84 tasks | 84 tasks | 84 tasks | 84 tasks | Clean execution, KSP processing Room schemas with applicationName index, config cache stored |

---

## Event Ingestion & Resolution Subsystem

```text
Keyboard (CustomKeyboardService)           Accessibility (AppAccessibilityService)
         │                                                  │
         ▼                                                  ▼
ImeEventPublisher                               AccessibilityEventFilter
         │                                                  │
         │                                                  ▼
         │                                      AccessibilityEventMapper
         │                                                  │
         │                                                  ▼
         │                                      AccessibilityEventPublisher
         │                                                  │
         └────────────────────────┬─────────────────────────┘
                                  ▼
                            EventProcessor (fail-safe background execution)
                                  ├── SessionGate.shouldProcess()
                                  ├── Active Session Validation
                                  ├── PrivacyFilter.sanitize()
                                  ├── ContextResolver.resolve() (in-memory cached)
                                  └── EventRepository.insertEvent() (defense-in-depth safety validation)
```

- `ImeEventPublisher`: Dispatches event candidates off the UI thread without blocking keystroke delivery.
- `AccessibilityEventFilter`: Selects permitted event types, rejects self-monitoring loops, and limits scroll storm frequency.
- `AccessibilityEventMapper`: Enforces content minimization, extracts interaction metadata, and identifies password widgets.
- `AccessibilityEventPublisher`: Asynchronously passes mapped raw events into `EventProcessor` on background `SupervisorJob` scope.
- `EventProcessor`: Enforces session state machine, multi-layer privacy filtering, and fail-safe handling.
- `ContextResolver`: Maps Android package names to human-readable application labels using a thread-safe `ConcurrentHashMap` cache.

---

## Setup & Onboarding Subsystem

```text
MainActivity (onResume) ────────► SetupStateManager.refresh()
                                           │
                        ┌──────────────────┼──────────────────┐
                        ▼                  ▼                  ▼
              SystemStatusChecker  PermissionCoordinator  SessionManager
               (Accessibility,        (Runtime             (Active
                IME status, DB)       permissions)          Session)
                        │                  │                  │
                        └──────────────────┼──────────────────┘
                                           ▼
                                 StateFlow<SetupState>
                                           │
                        ┌──────────────────┴──────────────────┐
                        ▼                                     ▼
                OnboardingScreen                         SetupScreen
            (Welcome & Disclosure)                  (Verification & Actions)
```

- `SetupState`: Immutable domain state evaluating `isComplete` and `isOperational`.
- `SystemStatusChecker`: Isolates Android system settings and service checks for deterministic JVM testing.
- `PermissionCoordinator`: Handles runtime permissions without trapping the user upon denial.
- `SetupStateManager`: Reactive coordinator reflecting real-time configuration changes.

---

## Dashboard Subsystem

```text
Room DB (SQLite Engine)        SetupStateManager        SessionManager
  │             │                      │                       │
  │ (COUNT/SUM) │ (Latest Event)       │                       │
  ▼             ▼                      ▼                       ▼
EventCounts   Latest Event         SetupState         SessionState + ActiveSession
  │             │                      │                       │
  └─────────────┴───────────────┬──────┴───────────────────────┘
                                ▼
                       DashboardViewModel
                    (combine() with Eagerly)
                                │
                                ▼
                       DashboardUiState
                                │
                                ▼
                        DashboardScreen
   ┌────────────────────────────────────────────────────────┐
   │ • System Capabilities: Keyboard, Accessibility, DB     │
   │ • Service Warning Banner (Non-blocking settings link)  │
   │ • Session Controls: Start, Pause, Resume, Stop actions │
   │ • Current App Context: "Notes App" (com.example.notes) │
   │ • Live Metric Counters: Total, Keys, Clicks, Scrolls   │
   │ • Quick Actions: Timeline, Settings, Setup Wizard      │
   └────────────────────────────────────────────────────────┘
```

- `EventCounts`: Immutable domain model tracking aggregate metrics (`total`, `keyboard`, `accessibility`, `clicks`, `focus`, `scrolls`, `windowChanges`).
- `EventCountsTuple`: Zero-memory Room tuple computed via direct SQLite `COUNT(*)` and conditional `SUM` expressions, completely avoiding entity deserialization into RAM.
- `DashboardUiState`: Reactive unified state providing live system capability indicators, foreground app context, and session status.
- `DashboardViewModel`: Central presentation coordinator ensuring real-time state delivery via `SharingStarted.Eagerly`.
- `DashboardScreen`: Material 3 edge-to-edge UI with responsive status badges and interactive session management.

---

## Event Timeline & Filtering Subsystem

```text
Room DB (SQLite Engine)        SessionRepository       ObserveActiveSessionUseCase
       │                               │                            │
       │ (LIMIT 50 OFFSET 50*page)     │ (observeAllSessions)       │
       ▼                               ▼                            ▼
List<InteractionEvent>       List<MonitoringSession>       ActiveSession
       │                               │                            │
       └───────────────────────────────┼────────────────────────────┘
                                       ▼
                              TimelineViewModel
                          (FlatMap & Combine with Eagerly)
                                       │
                                       ▼
                               TimelineUiState
                                       │
                                       ▼
                                 EventsScreen
   ┌─────────────────────────────────────────────────────────────┐
   │ • Search Bar (Immediate SQLite LIKE query execution)        │
   │ • Event Type Filter Chips: Typing, Click, Focus, Scroll     │
   │ • Multi-Dimensional Filter Dialog: Source, Package, Session │
   │ • Pagination Strip: Record range & boundary-safe navigation │
   │ • Chronological Timeline: Reverse-chronological event cards │
   │ • Event Detail Dialog: Full inspection & privacy notice     │
   └─────────────────────────────────────────────────────────────┘
```

- `EventFilterCriteria`: Immutable domain model capturing search query, package, event type, source, classification, session ID, and timestamp constraints.
- `InteractionEventDao.observeEventsFilteredPaged`: Paginated SQLite query with `LIMIT :limit OFFSET :offset` enforcing bounded heap memory consumption regardless of database size.
- `InteractionEventDao.observeFilteredCount`: Companion query computing total matching items for pagination calculation.
- `InteractionEventDao.observeDistinctPackageNames`: Real-time query populating package filter controls with observed packages.
- `TimelineUiState`: Comprehensive presentation model providing active filter state, page bounds, and event detail modal data.
- `TimelineViewModel`: Reactive coordinator ensuring immediate, bounded state delivery via `SharingStarted.Eagerly`.
- `EventsScreen`: Feature-complete Material 3 interface with search, filter chips, pagination, empty state handling, and detailed security disclosures.

---

## Database-Backed Search & Debounced Query Subsystem

```text
User Types Keystroke in Search Bar
          │
          ▼
   searchInputFlow (Immediate text update in UI — zero typing lag)
          │
          ├── debounce(300ms) [instant if query is blank]
          ▼
   searchQueryFlow (Debounced search term)
          │
          ├──────────────────────────────┬──────────────────────────────┐
          ▼                              ▼                              ▼
InteractionEventDao            InteractionEventDao            InteractionEventDao
.observeEventsFilteredPaged()   .observeFilteredCount()        .observeMatchingApplications()
(Multi-field index search)      (Matches count for pagination) .observeMatchingPackages()
          │                              │                              │
          └──────────────────────────────┼──────────────────────────────┘
                                         ▼
                                 TimelineViewModel
                            (SearchStateFlow & StateFlow)
                                         │
                                         ▼
                                  TimelineUiState
                    (events, totalCount, searchSuggestions)
                                         │
                                         ▼
                                   EventsScreen
     ┌─────────────────────────────────────────────────────────────────┐
     │ • Search Bar with real-time text input and instant clear        │
     │ • Auto-Suggestion Strip (Scrollable suggestion chips):          │
     │     - App: "Notes" / Pkg: "com.example.notes" / Type: "TYPING"  │
     │ • Instant filter application upon chip selection                │
     │ • Filtered Reverse-Chronological Event Stream                   │
     └─────────────────────────────────────────────────────────────────┘
```

- `InteractionEventEntity` Search Indexing: Added `@Index("applicationName")` to complement existing indices on `sessionId`, `timestamp`, `packageName`, and `eventType`. Generates SQLite index `index_interaction_events_applicationName` for sub-millisecond query execution.
- Multi-Field Coverage (SRS.md §50): Room queries match across `applicationName`, `packageName`, `eventType`, `sessionId`, and non-null `eventData`.
- Privacy Defense-in-Depth: Sensitive interaction events have `eventData = null` and `eventType = 'SENSITIVE_INPUT_DETECTED'`. They cannot be matched by credential string queries.
- Debounced Query Engine: 300ms debounce prevents SQLite I/O flooding during rapid typing while keeping the input field immediately responsive.
- Auto-Suggestions: `observeSearchSuggestions` queries distinct matching applications and packages directly from SQLite, capped at 10 items.

---

## Export

CSV (user-initiated local export)

---

## Dependencies (Pinned)

| Dependency | Exact Version | Purpose | Required? | APK Impact |
|---|---|---|---|---|
| Kotlin Stdlib | 2.0.21 | Language | Yes | Bundled |
| Compose BOM | 2024.12.01 | UI framework | Yes | ~2-3MB |
| Compose Material3 | (via BOM) | Design system | Yes | Included in Compose |
| Navigation Compose | 2.8.5 | Screen navigation | Yes | ~200KB |
| Lifecycle Runtime / ViewModel Compose | 2.8.7 | ViewModel integration | Yes | ~100KB |
| Room Runtime & KTX | 2.6.1 | Database | Yes | ~500KB |
| SQLCipher Android | 4.6.1 | Database encryption | Yes | ~3-5MB per ABI |
| SQLite KTX | 2.4.0 | Database support | Yes | ~20KB |
| Preferences DataStore | 1.1.1 | App preferences | Yes | ~50KB |
| KotlinX Coroutines Android | 1.9.0 | Async/reactive | Yes | ~200KB |
| Core KTX | 1.15.0 | AndroidX core utilities | Yes | ~100KB |

---

## Package Name

`com.example.inputmonitor`

See DEC-010.

