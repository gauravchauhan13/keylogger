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

| Metric | Phase 0 Baseline | Phase 1 Foundation | Phase 2 Data Foundation | Phase 3 Monitoring Sessions | Phase 4 Basic Keyboard | Phase 5 Complete Keyboard | Notes |
|---|---|---|---|---|---|---|---|
| Debug APK Size | 48,608,524 bytes (~46.35 MB) | 48,720,225 bytes (~46.46 MB) | 48,887,522 bytes (~46.62 MB) | 49,048,518 bytes (~46.78 MB) | 49,127,138 bytes (~46.85 MB) | 49,137,994 bytes (~46.86 MB) | Multi-ABI, unminified, Room + SQLCipher + DataStore + SessionManager + CustomKeyboardService |
| Release APK Size | 22,945,972 bytes (~21.88 MB) | 23,318,576 bytes (~22.24 MB) | 23,646,256 bytes (~22.55 MB) | 24,067,628 bytes (~22.95 MB) | 24,067,628 bytes (~22.95 MB) | 24,067,628 bytes (~22.95 MB) | Multi-ABI, R8 minified + shrunk resources. Delta ~0 KB for layout definitions |
| Unit Test Suite | 1 test (PASSED) | 3 suites / 5 tests (PASSED) | 8 suites / 17 tests (PASSED) | 11 suites / 37 tests (PASSED) | 14 suites / 52 tests (PASSED) | 14 suites / 66 tests (PASSED) | Added comprehensive multi-layout and shift state tests |
| Gradle Tasks Executed | 70 tasks | 89 tasks | 91 tasks | 94 tasks | 97 tasks | 97 tasks | Clean execution, KSP processing Room schemas, config cache stored |

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

