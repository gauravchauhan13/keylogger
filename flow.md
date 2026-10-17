# Application Flow

**Document:** `flow.md`
**Status:** Living Document / Authoritative
**Purpose:** How the application currently flows. Must remain synchronized with implementation.

---

# 1. Overall Application Flow

```text
INSTALL
   ↓
FIRST LAUNCH
   ↓
DISCLOSURE / ONBOARDING
   ↓
PERMISSIONS
   ↓
ACCESSIBILITY SETUP
   ↓
ENABLE KEYBOARD
   ↓
SELECT KEYBOARD
   ↓
VERIFY SETUP
   ↓
DASHBOARD
   ↓
START MONITORING SESSION
   ↓
COLLECT PERMITTED EVENTS
   ↓
EVENT PROCESSOR / SESSION GATE / CLASSIFICATION
   ↓
PRIVACY FILTER
   ↓
CONTEXT RESOLVER
   ↓
REPOSITORY SAFETY VALIDATION
   ↓
ENCRYPTED PERSISTENCE LAYER
   ↓
ROOM / LOCAL STORAGE
   ↓
VIEW / ANALYZE / EXPORT
```

---

# 2. First Installation Flow

```text
User installs APK
   ↓
Android registers application
   ↓
User opens application
   ↓
MonitoringApplication.onCreate()
   ↓
MainActivity launches
   ↓
Check onboarding completion (DataStore)
   ↓
Show Onboarding/Disclosure screen
   ↓
User acknowledges
   ↓
Navigate to Setup
```

---

# 3. Permission Flow

```text
Setup Screen
   ↓
Check required runtime permissions
   ↓
Request missing permissions
   ↓
User grants / denies
   ↓
SetupStateManager.refresh()
   ↓
Update UI state
```

---

# 4. Accessibility Setup Flow

```text
Setup Screen
   ↓
User taps "Enable Accessibility"
   ↓
Open Android Accessibility Settings
   ↓
User enables AppAccessibilityService
   ↓
User returns to app
   ↓
SetupStateManager.refresh()
   ↓
UI updates accessibility status
```

---

# 5. Keyboard Setup Flow

```text
Setup Screen
   ↓
User taps "Enable Keyboard"
   ↓
Open Android Input Method Settings
   ↓
User enables CustomKeyboardService
   ↓
User returns to app
   ↓
User taps "Select Keyboard"
   ↓
Show Android IME picker
   ↓
User selects CustomKeyboardService
   ↓
SetupStateManager.refresh()
   ↓
UI updates keyboard status
```

---

# 6. Keyboard Runtime Flow

```text
TEXT FIELD FOCUSED
   ↓
ANDROID STARTS IME
   ↓
onStartInput(EditorInfo)
   ↓
EditorInfoAnalyzer
   ↓
SensitiveInputDetector
   ↓
KeyboardController updates state
   ↓
Render appropriate layout
   ↓
USER PRESSES KEY
   ↓
KeyProcessor
   │
   ├──→ InputConnection → TARGET APP  (always, immediately)
   │
   └──→ Interaction Event Candidate (best-effort)
              ↓
         Session Gate (active session?)
              ↓
         Classification / SensitiveInputDetector
              ↓
         Privacy Filter
              ↓
         ContextResolver
              ↓
         Repository Safety Validation
              ↓
         Encrypted Persistence Layer
              ↓
         Room
```

---

# 7. Monitoring Session Flow & State Machine

```text
               ┌────────────────────────────────────────────────────────┐
               │                        INACTIVE                        │
               └───────────────────────────┬────────────────────────────┘
                                           │
                                  startSession(name, notes)
                                           │
                                           ▼
               ┌────────────────────────────────────────────────────────┐
               │                         ACTIVE                         │
               │   • PreferencesRepository.setActiveSessionId(id)       │
               │   • SessionGate.canProcessEvents() == true             │
               │   • UI badge: ● ACTIVE                                 │
               └───────────────┬────────────────────────▲───────────────┘
                               │                        │
                  pauseSession()                        resumeSession()
                               │                        │
                               ▼                        │
               ┌────────────────────────────────────────┴───────────────┐
               │                         PAUSED                         │
               │   • SessionGate.canProcessEvents() == false (DROP)     │
               │   • UI badge: ⏸ PAUSED                                 │
               └───────────────┬────────────────────────────────────────┘
                               │
                          stopSession()
                               │
                               ▼
               ┌────────────────────────────────────────────────────────┐
               │                        STOPPED                         │
               │   • endTime recorded in Room database                  │
               │   • PreferencesRepository.setActiveSessionId(null)     │
               │   • SessionGate.canProcessEvents() == false (DROP)     │
               │   • UI badge: ■ STOPPED                                │
               └────────────────────────────────────────────────────────┘

Process Restart Recovery Flow:
MonitoringApplication.onCreate()
   ↓
SessionManager.restoreSessionIfActive()
   ↓
PreferencesRepository.activeSessionId?
   ├── NULL ──► State remains INACTIVE
   └── ID   ──► Query SessionRepository.getSessionById(ID)
                  ├── State != STOPPED ──► Restore ACTIVE / PAUSED state
                  └── State == STOPPED ──► Clear preference, state STOPPED
```

## 7.1 Session Gate Flow

```text
Platform Service (IME / Accessibility)
             │
             ▼
    RawInteractionEvent
             │
             ▼
        SessionGate
             │
   canProcessEvents()?
             │
     ┌───────┴───────┐
     │               │
    YES              NO
(Session ACTIVE)  (INACTIVE / PAUSED / STOPPED)
     │               │
     ▼               ▼
Privacy Pipeline  DROP EVENT
     │            (Zero processing,
     ▼             zero persistence)
Encrypted DB
```

---

# 8. Accessibility Event Flow

```text
Android System
   ↓
onAccessibilityEvent()
   ↓
AccessibilityEventFilter (enabled category?)
   ├── Ignore → return
   └── Accept
         ↓
   AccessibilityEventMapper
         ↓
   RawInteractionEvent
         ↓
   Enqueue asynchronously → return from callback
         ↓
   EventProcessor (background)
         ↓
   (same pipeline as §6)
```

---

# 9. IME Keyboard Input Flow

```text
User touches key on KeyboardView
   │
   ├── Audio feedback (AudioManager.playSoundEffect)
   ├── Haptic feedback (HapticFeedbackConstants.KEYBOARD_TAP)
   │
   ▼
CustomKeyboardService.handleKeyAction(action)
   │
   ├── KeyAction.Shift ────────► KeyboardController.toggleShift() ─► refresh KeyboardView
   ├── KeyAction.SwitchIme ────► InputMethodManager.showInputMethodPicker()
   ├── KeyAction.SwitchLayout ─► KeyboardController.switchLayout() ─► refresh KeyboardView
   │
   └── KeyAction.Character / Space / Backspace / Enter
         │
         ▼
       KeyProcessor.processKey(action, currentInputConnection, editorInfo)
         │
         ├── Character ──► InputConnection.commitText(char, 1)
         │                   │
         │                   ▼ (if Shift was ON_ONCE, reset to OFF)
         │                 KeyboardController.onCharacterTyped()
         │
         ├── Space ──────► InputConnection.commitText(" ", 1)
         ├── Backspace ──► InputConnection.deleteSurroundingText(1, 0)
         └── Enter ──────► performEditorAction(actionId) or commitText("\n", 1)
```

## 9.1 Keyboard Layout Adaptation & Switching Flow

```text
Editor started (CustomKeyboardService.onStartInputView)
   │
   ▼
KeyboardController.onStartInputView(info)
   │
   ├── info.inputType & TYPE_CLASS_PHONE != 0 ──────────────► KeyboardLayoutType.PHONE
   ├── info.inputType & TYPE_CLASS_NUMBER != 0 ─────────────► KeyboardLayoutType.NUMERIC
   ├── info.inputType == TYPE_NULL ─────────────────────────► KeyboardLayoutType.NUMERIC
   ├── info.inputType & TYPE_MASK_VARIATION == EMAIL_ADDRESS ► KeyboardLayoutType.EMAIL
   ├── info.inputType & TYPE_MASK_VARIATION == URI ─────────► KeyboardLayoutType.URI
   └── default ─────────────────────────────────────────────► KeyboardLayoutType.QWERTY
   │
   └── Action Key Label: IME_ACTION_GO ("Go"), SEARCH ("Search"), SEND ("Send"),
       NEXT ("Next"), DONE ("Done"), multiline (↵)

Interactive Layout Switching:
[?123]   ──► KeyboardLayoutType.SYMBOLS
[=<\ ]   ──► KeyboardLayoutType.SYMBOLS_MORE
[ABC]    ──► KeyboardLayoutType.QWERTY (or EMAIL / URI based on context)
[123#]   ──► Toggle between NUMERIC and PHONE / SYMBOLS
Double-Tap [⇧] (within 350ms) ──► Caps Lock (⇪)
Single-Tap [⇧] ──► Shift Once (⬆) ──► Reverts to (⇧) on alphanumeric key press
```

---

# 10. Sensitive Input Flow

```text
ACTIVE EDITOR
   ↓
EditorInfo
   ↓
EditorInfoAnalyzer
   ↓
SensitiveInputDetector
   ↓
┌────────────────┐
│                │
NORMAL        SENSITIVE
│                │
▼                ▼
Normal event   CONTENT DISCARDED
processing     Metadata-only persisted
│                │
└────────┬───────┘
         ↓
  Event Processor
         ↓
  Repository
```

Classification = SENSITIVE implies:
- eventData = NULL
- Key preview popups suppressed
- Suggestion strip shows privacy badge
- Keyboard continues functioning normally

---

# 11. Database Flow

```text
Repository receives validated event
   ↓
Repository Safety Validation (enforce eventData = NULL if isSensitive)
   ↓
Map domain model → Room entity (sanitizes sensitive data)
   ↓
DatabaseKeyManager retrieves AES-GCM protected passphrase from Android Keystore
   ↓
SQLCipher SupportOpenHelperFactory opens encrypted SQLite database
   ↓
Write to encrypted .db file (input_monitor_encrypted.db)
   ↓
Room DAOs provide reactive Flow<List<Entity>> to Repositories
   ↓
Repositories map to domain models for UI/ViewModel consumption
```

---

# 12. Dashboard Flow

```text
Dashboard Screen
   ↓
ViewModel collects from repository
   ↓
Display: active session info, event counts, recent events
   ↓
User navigates to: Timeline / Statistics / Sessions / Export / Settings
```

---

# 13. Export Flow

```text
User initiates export
   ↓
Select session / data range
   ↓
Read from encrypted Room database
   ↓
Export Validation (re-check sensitive classifications)
   ↓
CSV Writer
   ↓
User selects destination via SAF (Storage Access Framework)
   ↓
Write CSV to user-selected location
```

---

# 14. Error/Failure Flow

```text
Database failure:
   → Keyboard continues functioning
   → Event persistence fails silently
   → Dashboard shows error state

Encryption key unavailable:
   → Database cannot open
   → Create new database + new key
   → Old data is lost (documented behavior)
   → Keyboard continues functioning

Event pipeline failure:
   → InputConnection delivery unaffected
   → Monitoring event dropped
   → Keyboard continues functioning

Privacy filter failure:
   → Content-bearing event NOT persisted (fail closed)
   → Keyboard continues functioning
```

---

# Current Implementation State

```text
Phase 0: Project initialization (COMPLETE)
Phase 1: Android foundation, navigation, edge-to-edge Compose UI (COMPLETE)
Phase 2: Data foundation, Room encrypted database, KeyStore AES-GCM, Repositories, defense-in-depth safety validation (COMPLETE)
Phase 3: Monitoring sessions, SessionManager state machine (INACTIVE <-> ACTIVE <-> PAUSED -> STOPPED), SessionGate event blocking, lifecycle use cases, Dashboard UI controls & badges (COMPLETE)
Phase 4: Custom keyboard foundation, CustomKeyboardService, QWERTY layout, InputConnection text delivery, shift state machine, audio/haptic feedback (COMPLETE)
Phase 5: Complete keyboard layouts (Symbols, Numbers, Phone, Email, URI, Caps lock, Editor actions) (COMPLETE)
Phase 6: Editor Analysis and Privacy (EditorInfoAnalyzer, SensitiveInputDetector, PrivacyFilter) (COMPLETE)
Phase 7: IME Event Pipeline (ImeEventPublisher, RawInteractionEvent emission, EventProcessor, ContextResolver, failure tolerance) (COMPLETE)
Phase 8: AccessibilityService (AppAccessibilityService, AccessibilityEventFilter, AccessibilityEventMapper, AccessibilityEventPublisher) (COMPLETE)
Phase 9: Unified Event Processing & Integration (Concurrent event interleaving, unified session gating, multi-pipeline privacy invariants, failure tolerance integration) (COMPLETE)
Phase 10: First-Run Setup & Onboarding UI (NEXT)
```
