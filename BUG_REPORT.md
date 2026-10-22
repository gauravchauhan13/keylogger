# DEFECT & BUG REPORT: InputMonitor

**Date:** 2026-10-22  
**Test Cycle:** Complete End-to-End Reliability, QA & Destructive Edge Case Audit  
**Application Version:** 0.1.0 (Build 1)  
**Target Environment:** Android 16 (API 36 Preview) / Host JDK 25  

---

## 1. Summary of Findings

- **CRITICAL Defects:** 0
- **HIGH Defects:** 0
- **MEDIUM Defects:** 1 (Tooling / Host JDK Compatibility)
- **LOW Defects:** 1 (API Deprecation Warning)
- **Crashes / ANRs:** 0

---

## 2. Bug Details

### BUG-001: Deprecated `getSuggestions` API in Android TextServicesSession
- **BUG-ID:** BUG-001
- **Severity:** LOW
- **Feature Area:** Custom Keyboard / Typing Suggestions
- **Component:** `com.example.inputmonitor.platform.ime.TypingSuggestions.kt`
- **Description:** During Kotlin compilation (`compileDebugKotlin`), a compiler warning is emitted:
  ```
  w: TypingSuggestions.kt:48:22 'fun getSuggestions(p0: TextInfo!, p1: Int): Unit' is deprecated. Deprecated in Java.
  ```
- **Steps to Reproduce:**
  1. Execute `./gradlew compileDebugKotlin`.
  2. Inspect compiler output.
- **Root Cause:** The Android SDK deprecated `getSuggestions(TextInfo, int)` in favor of `getSuggestions(TextInfo[], int, boolean)` in modern API levels.
- **Risk Assessment:** Non-blocking on current target SDK 35 and Android 16. Keyboard continues to supply suggestions correctly.
- **Recommended Remediation:** Migrate to the overloaded array-based `getSuggestions` method or fallback to local offline trie dictionary when spell checker session is inactive.
- **Status:** OPEN (Low Priority backlog).

---

### BUG-002: AGP Lint Analysis Fails on Host JDK 25 due to UAST Bytecode Incompatibility
- **BUG-ID:** BUG-002
- **Severity:** MEDIUM
- **Feature Area:** Build System / Android Lint
- **Component:** Android Gradle Plugin 8.7.0 `lintAnalyzeDebug`
- **Description:** Running `./gradlew lintDebug` on a developer machine with system default `Java 25.0.1 LTS` causes a worker crash in IntelliJ UAST parser:
  ```
  Execution failed for task ':app:lintAnalyzeDebug'.
  > A failure occurred while executing com.android.build.gradle.internal.lint.AndroidLintWorkAction > 25.0.1
  Caused by: java.lang.NoClassDefFoundError: Could not initialize class org.jetbrains.uast.UastFacade
  Caused by: Exception com.intellij.serviceContainer.AlreadyDisposedException
  ```
- **Steps to Reproduce:**
  1. Set system JAVA_HOME to JDK 25.
  2. Run `./gradlew lintDebug`.
- **Root Cause:** Android Gradle Plugin's embedded IntelliJ UAST bytecode visitor does not support Java 25 bytecode reflection yet.
- **Workaround:** Compile and test with JDK 17 (bundled in Android Studio JBR). Unit tests, Kotlin compilation, dexing, and APK assembly all succeed on JDK 25, but standalone lint requires JDK 17.
- **Status:** MITIGATED (Documented in setup guide).

---

## 3. Crash & ANR Review

No crashes, FATAL EXCEPTIONs, or Application Not Responding (ANR) events were observed during the entire testing scope, including:
- Fresh install & onboarding
- Live monitoring session start / pause / resume / stop
- 500-event aggressive UI monkey stress testing
- Native Share Sheet invocation
- Process death (`am force-stop`) and relaunch
