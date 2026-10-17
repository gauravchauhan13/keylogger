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

# 7. Monitoring Session Flow

```text
NONE
   ↓
User starts session → ACTIVE
   ↓
Events are processed and persisted
   ↓
User pauses → PAUSED
   ↓
Events not persisted while paused
   ↓
User resumes → ACTIVE
   ↓
User stops → STOPPED
   ↓
Session closed, no new events attach
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

# 9. IME Event Flow

```text
Key press
   ↓
KeyProcessor
   │
   ├──→ InputConnection.commitText() / sendKeyEvent()
   │
   └──→ Create RawInteractionEvent
              ↓
         EventProcessor
              ↓
         (same pipeline as §6)
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
Map domain model → Room entity
   ↓
SQLCipher encrypts via Room SupportSQLiteOpenHelper
   ↓
Write to encrypted .db file
   ↓
Android Keystore provides decryption key on read
   ↓
Room DAOs provide Flow<List<Entity>> for UI
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
Phase 0: Project initialization
No runtime flows implemented yet.
All flows above are planned architecture.
```
