# REGRESSION TEST REPORT: InputMonitor

**Date:** 2026-10-22  
**Test Scope:** Verification of existing system capabilities following the integration of Automatic Cloud Backup, FileProvider Share Sheet, and Custom Keyboard Emoji/Suggestion systems.  
**Baseline Build:** Commit `3e5b887`  
**Current Build:** Commit `b4030d6` / Working Tree  

---

## 1. Executive Summary

A full regression analysis was executed across the core monitoring engine, encrypted storage, privacy filter, user interface, and newly introduced backup/sharing features.

- **Total Regression Test Cases:** 62 test suites / 326 automated unit tests + 36 live journey checks.
- **Passed:** 362 (100%)
- **Failed:** 0
- **Regression Defects Introduced:** 0
- **Overall Regression Verdict:** **STABLE & VERIFIED**

---

## 2. Area-by-Area Regression Analysis

### A. Core Database & SQLCipher Encryption
- **Pre-existing Behavior:** Encrypted SQLite Room database using AES-256 with key managed by Android KeyStore.
- **Verification:**
  - Ran `DatabaseKeyManagerTest` (6 tests): Key generation, alias verification, and AES-256-GCM cipher initialization all passed.
  - Ran `SessionRepositoryTest` and `EventRepositoryTest`: Verified CRUD operations, composite indexing, and batch event inserts.
  - Live Process Death Test: Terminated process via `am force-stop` and relaunched. Re-opened database with 100% record retention.
- **Status:** **PASS — ZERO REGRESSION**

### B. Privacy Defense-in-Depth & Credential Scrubbing
- **Pre-existing Behavior:** Strict exclusion of passwords, PINs, OTPs, and credit card numbers from database storage (`eventData = NULL`).
- **Verification:**
  - Ran `SensitiveInputDetectorTest` (14 tests) and `PrivacyFilterTest` (18 tests).
  - Executed live `TestBenchScreen` evaluation on Android 16: Evaluated 7 input fields (including Password and Numeric PIN).
  - Ground-truth evaluation passed: Invariants AC-29, AC-30, AC-31, and AC-32 all verified on live device.
- **Status:** **PASS — ZERO REGRESSION**

### C. Custom Keyboard (IME) & Interaction Monitoring
- **Pre-existing Behavior:** Low-latency typing and accessibility event logging.
- **New Additions:** Typing suggestions, suggestion policy, emoji catalog, and emoji picker.
- **Verification:**
  - Ran `KeyProcessorTest`, `EmojiCatalogTest`, `EmojiSearchTest`, `LocalSuggestionsTest`, and `SuggestionPolicyTest`.
  - Live typing test on Android 16: Custom keyboard was selected as active IME, typed text into Settings search bar, verified that typing operated smoothly without latency, and verified event counts incremented cleanly.
- **Status:** **PASS — ZERO REGRESSION**

### D. Export Subsystem (SAF File Picker & Share Sheet)
- **Pre-existing Behavior:** Manual CSV generation via Android Storage Access Framework (SAF).
- **New Additions:** Native system Share Sheet using `FileProvider` and `ACTION_SEND`.
- **Verification:**
  - Ran `CsvEventExporterImplTest` (6 tests) and `ExportSessionUseCaseTest` (10 tests).
  - Live Share Sheet invocation on Android 16: Tapped "Share CSV via Android Share Sheet", successfully generated content URI `content://com.example.inputmonitor.fileprovider/exports/...`, and opened system chooser with Gmail and Google Drive targets.
- **Status:** **PASS — ZERO REGRESSION**

### E. Automatic Daily Cloud Backup Subsystem
- **New Feature Addition:** WorkManager periodic 24-hour backup, O(1) heap TLS streaming, and Android KeyStore token storage.
- **Verification:**
  - Ran 5 new automated test suites:
    - `BackupFileManagerTest` (206 lines)
    - `HttpBackupProviderTest` (196 lines)
    - `BackupSettingsRepositoryTest` (138 lines)
    - `AutoBackupSchedulerTest` (68 lines)
    - `AutoBackupWorkerTest` (242 lines)
  - All 326 unit tests passed in 53s.
- **Status:** **PASS — ZERO REGRESSION**

---

## 3. Regression Verdict

All existing features continue to operate with 100% stability. No broken code, no performance regressions, and no security regressions were detected.
