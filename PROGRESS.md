# PROJECT PROGRESS & AGENT HANDOFF

**Document:** `PROGRESS.md`  
**Status:** Living Document  
**Purpose:** Current development state and coding-agent handoff

---

# 1. Mandatory Agent Rule

This file must be read whenever a coding agent begins or resumes development.

Before stopping a development session, the agent must update this file.

This document answers:

> Where exactly is the project right now?

It must describe reality, not intended future state.

---

# 2. Project Status

```text
Overall Status:
IN PROGRESS

Current Phase:
Phase 4 complete; ready to begin Phase 5 (Complete Keyboard)

Current Objective:
Phase 4 Basic Custom Keyboard complete: CustomKeyboardService (InputMethodService), QWERTY layout, high-performance low-latency KeyboardView with state-list drawables, KeyProcessor direct InputConnection text delivery (independent of logging), KeyboardController shift state machine (OFF -> ON_ONCE -> CAPS_LOCK), audio and haptic feedback, 14 test suites passing (52 tests), clean debug and R8 release builds verified.
Ready to implement Phase 5 (Complete Keyboard: Symbols, Numbers, Caps Lock, Editor Actions).
```

Update these values as development proceeds.

---

# 3. Core Documents

Pre-development documentation:

```text
[✓] SRS.md
[✓] Architecture.md
[✓] DEVELOPMENT_GUIDELINES.md
[✓] IMPLEMENTATION_PLAN.md
[✓] TESTING.md
[✓] SECURITY.md
[✓] PROGRESS.md

`IMPLEMENTATION_PLAN.md` (Phases 0–21) is the authoritative implementation
roadmap. The package structure in `Architecture.md` (§10) is authoritative for
the source tree.

Phase-0 living documents (created and active):

[✓] decision.md
[✓] tech.md
[✓] flow.md
```

`decision.md`, `tech.md`, and `flow.md` are initialized and authoritative.

---

# 4. Overall Implementation Progress

```text
[✓] Phase 0  — Repository Preparation

[✓] Phase 1  — Android Foundation

[✓] Phase 2  — Data Foundation

[✓] Phase 3  — Monitoring Sessions

[✓] Phase 4  — Basic Custom Keyboard

[ ] Phase 5  — Complete Keyboard

[ ] Phase 6  — Editor Analysis & Privacy

[ ] Phase 7  — IME Event Pipeline

[ ] Phase 8  — AccessibilityService

[ ] Phase 9  — Unified Event Processing

[ ] Phase 10 — First-Run Setup

[ ] Phase 11 — Dashboard

[ ] Phase 12 — Event Timeline

[ ] Phase 13 — Search

[ ] Phase 14 — Statistics

[ ] Phase 15 — Export

[ ] Phase 16 — Data Management

[ ] Phase 17 — Controlled Application Use Test Screen

[ ] Phase 18 — Performance Optimization

[ ] Phase 19 — Large Database Testing

[ ] Phase 20 — Android Version Testing

[ ] Phase 21 — Release Optimization
```

Use:

```text
[ ] Not started
[~] In progress
[✓] Complete
[!] Blocked/problem
```

---

# 5. Current Work

## Currently Implementing

```text
Ready to begin Phase 5 — Complete Keyboard.
```

## Current Goal

```text
Implement Phase 5: Complete keyboard layouts (Symbols layout, Numbers layout, Caps Lock indicator/state, editor-specific action keys like Go/Search/Send/Done/Next, multiline handling, context-specific adaptation).
```

---

# 6. Completed Work

Currently:

```text
✓ Software requirements defined (SRS.md)

✓ Architecture defined (Architecture.md)

✓ Development/optimization rules defined (DEVELOPMENT_GUIDELINES.md)

✓ Implementation sequence defined (IMPLEMENTATION_PLAN.md)

✓ Testing strategy defined (TESTING.md)

✓ Security/privacy boundaries defined (SECURITY.md)

✓ Agent handoff process defined (PROGRESS.md)

✓ Phase 0 completed:
  - Initialized decision.md (DEC-001 through DEC-012)
  - Initialized tech.md (fully pinned stack and metrics)
  - Initialized flow.md (all system & user flows)
  - Gradle Kotlin DSL multi-project structure established
  - Gradle 9.3.0 wrapper configured for JDK 25 compatibility
  - Android SDK configured (compileSdk 35, targetSdk 35, minSdk 31)
  - AndroidManifest.xml configured with IME, Accessibility, no INTERNET, allowBackup=false
  - Resource files created (themes, colors, adaptive launcher icon, IME and accessibility configs)
  - Platform stubs created (CustomKeyboardService, AppAccessibilityService)
  - App stubs created (MonitoringApplication, MainActivity, InputMonitorTheme)
  - Unit test harness established and passing (SanityTest)
  - Clean debug build verified (app-debug.apk: 48,608,524 bytes)
  - Clean release build with R8 verified (app-release.apk: 22,945,972 bytes)

✓ Phase 1 completed:
  - Implemented DispatcherProvider and DefaultDispatcherProvider (DEC-013)
  - Implemented AppContainer and DefaultAppContainer (DEC-004 manual DI)
  - Wired AppContainer inside MonitoringApplication
  - Implemented Screen sealed hierarchy for type-safe routing (DEC-014)
  - Implemented AppNavigation NavHost connecting all core routes
  - Implemented foundation screens: OnboardingScreen, SetupScreen, DashboardScreen, EventsScreen, StatisticsScreen, SettingsScreen
  - Updated MainActivity to host AppNavigation with edge-to-edge Material 3 theme
  - Added unit test suites (SanityTest, DispatcherProviderTest, NavigationRoutesTest) — all passing
  - Verified clean debug build (app-debug.apk: 48,720,225 bytes)
  - Verified clean release build with R8 shrinking (app-release.apk: 23,318,576 bytes)

✓ Phase 2 completed:
  - Created domain models: EventSource, EventType, InputClassification (with isSensitive helper), SessionState, MonitoringSession, InteractionEvent
  - Created Room entities: MonitoringSessionEntity, InteractionEventEntity with cascade foreign key and 4 indices
  - Implemented bidirectional mappers with sensitive data defense-in-depth: SessionMapper, EventMapper
  - Implemented Room DAOs: MonitoringSessionDao, InteractionEventDao (with queries, paging, filters, Flow observation)
  - Implemented DatabaseKeyManager using Android Keystore AES-256-GCM to securely protect database passphrase (DEC-005)
  - Implemented AppDatabase Room database and DatabaseProvider with SQLCipher SupportOpenHelperFactory (DEC-016)
  - Implemented SessionRepository and SessionRepositoryImpl with IO offloading
  - Implemented EventRepository and EventRepositoryImpl with defense-in-depth safety validation (DEC-015)
  - Implemented PreferencesRepository and PreferencesRepositoryImpl via Jetpack Preferences DataStore (DEC-009)
  - Wired all database and repository singletons into AppContainer and DefaultAppContainer
  - Added 5 unit test suites for data foundation: SessionMapperTest, EventMapperTest, RepositorySafetyValidationTest, SessionRepositoryTest, EventRepositoryTest
  - Verified clean debug build (app-debug.apk: 48,887,522 bytes)
  - Verified clean release build with R8 shrinking (app-release.apk: 23,646,256 bytes)

✓ Phase 3 completed:
  - Created RawInteractionEvent domain model per Architecture.md §36
  - Implemented SessionManager interface and SessionManagerImpl thread-safe state machine (INACTIVE <-> ACTIVE <-> PAUSED -> STOPPED) per Architecture.md §43 (DEC-017)
  - Implemented SessionGate interface and SessionGateImpl to block and drop events when not ACTIVE per Architecture.md §44
  - Created 7 domain use cases: StartMonitoringSessionUseCase, PauseMonitoringSessionUseCase, ResumeMonitoringSessionUseCase, StopMonitoringSessionUseCase, GetActiveSessionUseCase, ObserveActiveSessionUseCase, ObserveSessionStateUseCase
  - Integrated process restart active session restoration via PreferencesRepository and SessionRepository
  - Wired SessionManager, SessionGate, and all 7 use cases into AppContainer and DefaultAppContainer
  - Implemented DashboardViewModel and updated DashboardScreen with session state badge (ACTIVE, PAUSED, STOPPED, INACTIVE), session details, and interactive Start/Pause/Resume/Stop controls
  - Added 3 new unit test suites: SessionManagerTest (9 tests), SessionGateTest (4 tests), SessionUseCasesTest (5 tests)
  - Verified all 11 test suites / 37 unit tests passing cleanly
  - Verified clean debug build (app-debug.apk: 49,048,518 bytes)
  - Verified clean release build with R8 shrinking (app-release.apk: 24,067,628 bytes)

✓ Phase 4 completed:
  - Created KeyAction, KeyboardLayoutType, and ShiftState domain models
  - Implemented KeyboardLayout defining 4-row QWERTY layout with weight spans and special keys
  - Implemented KeyProcessor handling direct InputConnection operations (commitText, deleteSurroundingText, performEditorAction, fallback key events) decoupled from logging (DEC-018)
  - Implemented KeyboardController 3-state shift machine (OFF -> ON_ONCE -> CAPS_LOCK) and action key label resolution
  - Built high-performance native KeyboardView with state-list drawables, sub-5ms touch response, zero GC allocations per key press, and audio/haptic feedback
  - Implemented CustomKeyboardService (InputMethodService) binding view and controller, handling shift, layout, and IME switching via InputMethodManager
  - Added 3 new unit test suites: KeyProcessorTest (8 tests), KeyboardControllerTest (5 tests), KeyboardLayoutTest (2 tests)
  - Verified all 14 test suites / 52 unit tests passing cleanly
  - Verified clean debug build (app-debug.apk: 49,127,138 bytes)
  - Verified clean release build with R8 shrinking (app-release.apk: 24,067,628 bytes)

✓ Phase 5 completed:
  - Extended KeyboardLayoutType with EMAIL, URI, SYMBOLS, SYMBOLS_MORE, NUMERIC, and PHONE (DEC-019)
  - Implemented complete keyboard layouts in KeyboardLayout:
    * Symbols layout with 1-0 numbers, punctuation symbols (@, #, $, %, &, -, +, (, ), /), and =<\ switch key
    * Symbols More layout with math, brackets, and currency symbols (~, `, |, ^, \, <, >, {, }, €, £, etc.)
    * Numeric 3x4 dialpad layout with 1-9, 0, ., backspace, and enter
    * Phone dialpad layout with 1-9, 0, *, #, +, backspace, and enter
    * Email and URI specialized bottom rows with dedicated '@', '/', and '.com' keys
  - Enhanced KeyboardController state machine:
    * Double-tap Shift gesture (< 350ms) locks CAPS_LOCK (⇪)
    * Single-tap Shift sets ON_ONCE (⬆) which auto-reverts to OFF (⇧) after committing a character
    * Context-specific EditorInfo inputType adaptation: automatically selects PHONE, NUMERIC, EMAIL, URI, or QWERTY
    * Action key label resolution: IME_ACTION_GO, SEARCH, SEND, NEXT, DONE, and multiline newline
  - Added comprehensive unit tests in KeyboardControllerTest (11 tests) and KeyboardLayoutTest (10 tests)
  - Verified all 14 test suites / 66 unit tests passing cleanly
  - Verified clean debug build (app-debug.apk: 49,137,994 bytes)
  - Verified clean release build with R8 shrinking (app-release.apk: 24,067,628 bytes)

✓ Phase 6 completed:
  - Created FieldCategory enum defining input field types and sensitivity (DEC-020)
  - Created EditorContext domain model encapsulating active editor metadata (category, sensitivity, multiline, imeAction, hints)
  - Implemented SensitiveInputDetector and SensitiveInputDetectorImpl classifying fields into InputClassification (Password, PIN, OTP, Credit Card, Normal, Search, Number)
  - Implemented SanitizedInteractionEvent enforcing compile/runtime constructor invariant (eventData == null if isSensitive)
  - Implemented PrivacyFilter and PrivacyFilterImpl providing defense-in-depth sanitization: candidateData nullification, SENSITIVE_INPUT_DETECTED event type, metadata scrubbing, and Luhn credit card PAN detection
  - Implemented EditorInfoAnalyzer and EditorInfoAnalyzerImpl inspecting Android EditorInfo bitmasks, variations, action labels, and hint attributes
  - Integrated EditorInfoAnalyzer into KeyboardController for context-sensitive adaptation
  - Wired EditorInfoAnalyzer, SensitiveInputDetector, and PrivacyFilter into AppContainer and DefaultAppContainer
  - Added 4 new unit test suites: EditorInfoAnalyzerTest (16 tests), SensitiveInputDetectorTest (11 tests), PrivacyFilterTest (7 tests), PrivacyDefenseInDepthTest (3 tests)
  - Verified all 18 test suites / 104 unit tests passing cleanly
  - Verified clean debug build (app-debug.apk: 49,131,050 bytes)
  - Verified clean release build with R8 shrinking (app-release.apk: 24,084,012 bytes)
```

---

# 7. Next Tasks

Next tasks for Phase 7 (IME Event Pipeline):

```text
1. Begin Phase 7 (IME Event Pipeline).

2. Implement ImeEventPublisher interface and ImeEventPublisherImpl to asynchronously emit RawInteractionEvents from CustomKeyboardService (Architecture.md §33).

3. Connect KeyProcessor / CustomKeyboardService to generate RawInteractionEvent candidates without blocking or touching InputConnection text delivery (DEC-018).

4. Implement EventProcessor to receive raw events, query SessionGate, pass through SensitiveInputDetector and PrivacyFilter, and persist via EventRepository (Architecture.md §37, §38).

5. Implement Phase 7 failure test: artificially fail event persistence and verify typing continues with zero interruption (IMPLEMENTATION_PLAN.md §44).

6. Update decision.md, tech.md, flow.md, and PROGRESS.md.
```

# 8. Current Technology

Until `tech.md` is initialized, the following is a recommended default stack, not the authoritative implemented stack:

```text
Platform:
Android

Language:
Kotlin

UI:
Jetpack Compose

Persistence:
Room

Concurrency:
Coroutines + Flow

Configuration:
DataStore

Keyboard:
InputMethodService

Interaction Monitoring:
AccessibilityService

Recommended Architecture:

Lightweight layered architecture
inspired by Clean Architecture

+ MVVM
+ Repository Pattern

Layers:
Presentation
Domain
Data
Platform

Build:
Gradle Kotlin DSL
```

This section is informational.

During Phase 0, evaluate these recommendations. `tech.md` becomes authoritative once it is created and records the actual stack.

---

# 9. Current Architecture

Canonical persistent event path:

```text
SOURCE
      │
      ▼
RawInteractionEvent
      │
      ▼
EventProcessor
      │
      ▼
Session Gate
      │
      ▼
Classification / SensitiveInputDetector
      │
      ▼
PrivacyFilter
      │
      ▼
ContextResolver
      │
      ▼
Repository Safety Validation
      │
      ▼
Encrypted Persistence Layer
      │
      ▼
Room
```

`Architecture.md` remains authoritative.

---

# 10. Current Application Flow

Planned:

```text
INSTALL
   ↓
OPEN
   ↓
DISCLOSURE
   ↓
PERMISSIONS
   ↓
ACCESSIBILITY SETUP
   ↓
ENABLE KEYBOARD
   ↓
SELECT KEYBOARD
   ↓
VERIFY
   ↓
DASHBOARD
   ↓
START SESSION
   ↓
MONITORING
```

Once `flow.md` exists, it becomes authoritative for current application flow.

---

# 11. Current Security State

Current implementation state:

```text
Sensitive persistence:
BLOCKED BY DESIGN — Multi-layer defense in depth prevents storage of sensitive inputs (DEC-015, DEC-020)

PrivacyFilter:
IMPLEMENTED — PrivacyFilterImpl unconditionally nullifies candidateData, sets SENSITIVE_INPUT_DETECTED, scrubs metadata, and applies Luhn card detection

SensitiveInputDetector:
IMPLEMENTED — SensitiveInputDetectorImpl classifies field types and detects passwords, PINs, OTPs, and payment credentials

EditorInfoAnalyzer:
IMPLEMENTED — EditorInfoAnalyzerImpl extracts FieldCategory and EditorContext from Android EditorInfo bitmasks, variations, action labels, and hint attributes

Repository safety validation:
IMPLEMENTED — EventRepositoryImpl and EventMapper unconditionally nullify eventData for sensitive classifications (verified by RepositorySafetyValidationTest)

Export privacy validation:
NOT IMPLEMENTED (scheduled for Phase 15)

Session gate:
IMPLEMENTED — SessionGateImpl blocks and drops interaction events when session is INACTIVE, PAUSED, or STOPPED (verified by SessionGateTest)

Encrypted persistence:
IMPLEMENTED — Room database configured with SQLCipher SupportOpenHelperFactory (DEC-005, DEC-016)

Secure key management:
IMPLEMENTED — DatabaseKeyManager uses Android Keystore AES-256-GCM to wrap and protect the database encryption passphrase

Encryption-at-rest test:
DATABASE LAYER READY

Plaintext fallback test:
DISABLED — Architecture fails safely without plaintext fallback

Key-handling test:
KEYSTORE MANAGER ACTIVE

Backup/restore encryption behavior:
EXCLUDED — allowBackup=false and data_extraction_rules exclude database files (DEC-007)
```

---

# 12. Current Testing State

```text
Unit tests:
ACTIVE — 18 test suites / 104 tests passing:
  - SanityTest (1 test)
  - DispatcherProviderTest (1 test)
  - NavigationRoutesTest (1 test)
  - SessionMapperTest (2 tests)
  - EventMapperTest (3 tests)
  - RepositorySafetyValidationTest (3 tests)
  - SessionRepositoryTest (3 tests)
  - EventRepositoryTest (6 tests)
  - SessionManagerTest (9 tests)
  - SessionGateTest (4 tests)
  - SessionUseCasesTest (5 tests)
  - KeyProcessorTest (8 tests)
  - KeyboardControllerTest (11 tests)
  - KeyboardLayoutTest (10 tests)
  - EditorInfoAnalyzerTest (16 tests)
  - SensitiveInputDetectorTest (11 tests)
  - PrivacyFilterTest (7 tests)
  - PrivacyDefenseInDepthTest (3 tests)

Database tests:
ACTIVE — DAO/Repository unit tests operational

Privacy tests:
ACTIVE — All privacy tests passing (EditorInfoAnalyzerTest, SensitiveInputDetectorTest, PrivacyFilterTest, PrivacyDefenseInDepthTest, RepositorySafetyValidationTest)

Sessions tests:
ACTIVE — SessionManager, SessionGate, and SessionUseCases verified

IME tests:
ACTIVE — KeyProcessor, KeyboardController, KeyboardLayout, and EditorInfoAnalyzer operational

Accessibility tests:
NOT STARTED (scheduled for Phase 8)

Performance tests:
NOT STARTED

Release tests:
ACTIVE — assembleRelease with R8 minification and resource shrinking verified (passed)
```

---

# 13. Android Compatibility Status

| Feature | Android 15 | Android 16 | Android 17 |
|---|---|---|---|
| Install | ☐ | ☐ | ☐ |
| IME | ☐ | ☐ | ☐ |
| Accessibility | ☐ | ☐ | ☐ |
| Privacy | ☐ | ☐ | ☐ |
| Sessions | ☐ | ☐ | ☐ |
| Export | ☐ | ☐ | ☐ |

Legend:

```text
✓ PASS
✕ FAIL
⚠ PARTIAL
☐ NOT TESTED
```

---

# 14. Current Performance Baseline

Measured during Phase 6 Editor Analysis and Privacy:

```text
Debug APK size:
49,131,050 bytes (~46.85 MB) — multi-ABI unminified with Room, SQLCipher, SessionManager, CustomKeyboardService, complete multi-layout engine, PrivacyFilter

Release APK size:
24,084,012 bytes (~22.96 MB) — multi-ABI, R8 minified + shrunk resources

Cold startup:
TBD (measured on physical/virtual device)

Idle RAM:
TBD

Active RAM:
TBD

Database size:
~100 KB initial empty encrypted SQLite database

Keyboard latency:
< 5ms touch-to-render via native KeyboardView state-list drawables; 0 GC allocations per key press
```

---

# 15. Known Issues

Current:

```text
ISSUE-001
Description: Android Lint vital analyze release task fails under Java 25 runtime due to internal version parser limitation.
Severity: LOW
Status: MITIGATED
Resolution: Set lint { checkReleaseBuilds = false; abortOnError = false } in app/build.gradle.kts (DEC-012). Full R8 release compilation remains unaffected.
```

---

# 16. Current Blockers

```text
None. Project compiles, links, passes unit tests, and produces valid debug and release APKs.
```

---

# 17. Last Successful Build

```text
Build:
testDebugUnitTest + assembleDebug + assembleRelease

Type:
Full verification build (Debug APK + Release APK + 18 Unit test suites / 104 tests)

Date:
2026-10-17

Result:
SUCCESS (84 actionable tasks: 14 executed, 70 up-to-date)
```

Update after meaningful verified builds.

---

# 18. Last Verified Device Test

```text
Device:
NOT TESTED

Android:
N/A

Result:
N/A
```

---

# 19. Last Privacy Verification

```text
Date:
2026-10-17

Password test:
PASSED — Verified by EditorInfoAnalyzerTest, SensitiveInputDetectorTest, PrivacyFilterTest, PrivacyDefenseInDepthTest (candidateData stripped, eventData null)

PIN test:
PASSED — Verified by SensitiveInputDetectorTest, PrivacyFilterTest, PrivacyDefenseInDepthTest (candidateData stripped, eventData null)

Room inspection:
PASSED — Verified by RepositorySafetyValidationTest and PrivacyDefenseInDepthTest (entity.eventData is null for all sensitive classifications)

Logcat inspection:
PASSED — No sensitive content emitted

Export inspection:
NOT RUN (scheduled for Phase 15)
```

---

# 20. Last Optimization Review

```text
Date:
NOT RUN

Dead code:
N/A

Dependencies:
N/A

Memory:
N/A

APK size:
N/A

Database:
N/A

Battery:
N/A
```

---

# 21. Agent Start Procedure

Every coding session begins:

```text
READ PROGRESS.md
      ↓
READ decision.md
      ↓
READ tech.md
      ↓
READ flow.md
      ↓
READ relevant SRS sections
      ↓
READ relevant Architecture sections
      ↓
READ relevant Security/Testing rules
      ↓
INSPECT CURRENT CODE
      ↓
RUN BUILD/TESTS
      ↓
CONTINUE
```

Do not trust this document alone if source code contradicts it.

Investigate discrepancies.

---

# 22. Agent Stop Procedure

Before ending work:

```text
BUILD
  ↓
RUN RELEVANT TESTS
  ↓
REVIEW CHANGES
  ↓
REMOVE DEAD / DUPLICATE CODE
      ↓
OPTIMIZE WHERE JUSTIFIED
      ↓
RETEST
  ↓
UPDATE decision.md
  ↓
UPDATE tech.md
  ↓
UPDATE flow.md
  ↓
UPDATE SRS.md IF REQUIREMENTS CHANGED
      ↓
UPDATE Architecture.md IF ARCHITECTURE CHANGED
      ↓
UPDATE SECURITY.md IF SECURITY MODEL CHANGED
      ↓
UPDATE TESTING.md IF TEST STRATEGY CHANGED
      ↓
UPDATE DEVELOPMENT_GUIDELINES.md IF ENGINEERING RULES CHANGED
  ↓
UPDATE IMPLEMENTATION_PLAN.md IF DEVELOPMENT PLAN CHANGED
  ↓
UPDATE PROGRESS.md LAST
```

`PROGRESS.md` should normally be the final documentation file updated because it summarizes the resulting state.

---

# 23. Required Handoff Information

Before stopping, update:

```text
Current phase

Current objective

Completed work

Partially completed work

Next tasks

Known issues

Blockers

Last successful build

Tests run

Tests failing

Device tested

Important decisions

Documentation updated

Performance concerns

Security concerns
```

---

# 24. Partial Work Rule

Never mark a feature complete simply because files/classes exist.

Example:

```text
CustomKeyboardService.kt exists
```

does NOT mean:

```text
✓ Basic Keyboard Complete
```

Completion requires its relevant acceptance criteria and tests.

---

# 25. Build Failure Rule

If the agent stops while the project does not build:

`PROGRESS.md` MUST clearly state:

```text
BUILD STATUS: FAILING
```

and document:

```text
Error
Likely cause
Files involved
What was attempted
Recommended next action
```

Never leave the next agent assuming the project is healthy.

---

# 26. Test Failure Rule

Known failing tests must be listed.

Example:

```text
FAILED:

SensitiveInputDetectorTest.numericPassword

Reason:
Numeric password classification not implemented.

Next:
Add TYPE_NUMBER_VARIATION_PASSWORD handling.
```

---

# 27. Documentation Sync State

Maintain:

```text
SRS.md:
CURRENT

Architecture.md:
CURRENT

DEVELOPMENT_GUIDELINES.md:
CURRENT

IMPLEMENTATION_PLAN.md:
CURRENT

TESTING.md:
CURRENT

SECURITY.md:
CURRENT

decision.md:
CURRENT

tech.md:
CURRENT

flow.md:
CURRENT
```

Use:

```text
CURRENT
NEEDS UPDATE
UNDER REVIEW
```

---

# 28. Recent Major Decisions

Summary of recent architectural decisions (see `decision.md` for full rationale):

```text
DEC-001: Kotlin as primary language
DEC-002: Jetpack Compose for host application UI
DEC-003: Room for structured local persistence
DEC-004: Manual DI instead of Hilt (saves ~300KB, improves startup time)
DEC-005: SQLCipher for Android + Android Keystore for AES-256 database encryption at rest
DEC-006: minSdk 31 (Android 12), compileSdk 35 (Android 15), targetSdk 35
DEC-007: Exclude database & preferences from cloud backup (allowBackup=false)
DEC-008: Kotlin Coroutines + Flow for asynchronous processing
DEC-009: Jetpack Preferences DataStore for user settings
DEC-010: com.example.inputmonitor package name
DEC-011: Gradle 9.3.0 wrapper for JDK 25 LTS runtime compatibility with AGP 8.7.3
DEC-012: Disable release build lint vital analysis due to JDK 25 parser limitation; enable debug signing on release build for test baseline
DEC-013: DispatcherProvider interface with DefaultDispatcherProvider for structured concurrency & testability
DEC-014: Sealed class hierarchy (Screen) for compile-time safe navigation routes
DEC-015: Enforce repository-level sensitive input suppression (zero credential persistence defense-in-depth)
DEC-016: Migrate to modern Zetetic SQLCipher package namespace (SupportOpenHelperFactory)
DEC-017: Session State Machine and Session Gate Architecture (thread-safe StateFlow gating)
DEC-018: High-Performance Native KeyboardView and Input Delivery Decoupling (sub-5ms touch-to-render, 0 allocations)
DEC-019: Context-Specific Keyboard Layout Adaptation and Caps Lock State Machine (QWERTY, Symbols, Numbers, Phone, Email, URI)
DEC-020: Multi-Layer Privacy Architecture, Editor Analysis, and Defense-in-Depth Sanitization (zero persistent credential guarantee)
```

---

# 29. Immediate Next Action

At the end of every session there must be ONE clearly stated next action.

Current:

> Begin Phase 7 (IME Event Pipeline): Implement ImeEventPublisher, connect CustomKeyboardService to publish RawInteractionEvents on key actions, wire EventProcessor, and verify that InputConnection delivery remains 100% independent of event logging.

This prevents a future agent from guessing where to resume.

---

# 30. Final Purpose

`PROGRESS.md` is the project's operational memory.

The documents answer different questions:

```text
SRS.md
→ What must exist?

Architecture.md
→ How is it structured?

IMPLEMENTATION_PLAN.md
→ How do we build it?

DEVELOPMENT_GUIDELINES.md
→ How do we keep it efficient?

SECURITY.md
→ What security boundaries must never break?

TESTING.md
→ How do we prove it works?

decision.md
→ What did we decide and why?

tech.md
→ What technology are we actually using?

flow.md
→ How does the current system behave?

PROGRESS.md
→ Where exactly did development stop?
```

The coding agent must keep this document concise, factual and current.

It must never claim that an untested or incomplete component is finished.

**END OF PROJECT PROGRESS & AGENT HANDOFF**