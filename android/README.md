# FocusLoop Android core prototype

Open this `android/` directory in Android Studio, select JDK 17, install SDK Platform 35 and Build Tools 35.0.0, and sync. The root project outside this directory contains the approved specifications and references. Package/application ID remains `dev.focusloop.spike` so the installed Phase 1 app can upgrade while retaining its data. Version 0.2-core (code 2), minimum API 26, compile/target API 35. This is a core prototype with diagnostic UI, not a release.

Pinned tooling: Gradle 8.11.1 (checksum pinned), AGP 8.9.1, Kotlin/Compose compiler 2.0.21, Compose BOM 2024.10.00, Activity 1.9.3, Lifecycle 2.8.7, coroutines 1.9.0, Room 2.7.2 and KSP 2.0.21-1.0.28. Room 2.7 fits Kotlin 2.0; no Room major/toolchain migration was needed. `app/schemas/` contains the exported Room schema and must remain version-controlled. No destructive migration fallback or main-thread database queries are enabled. Release targeting and dependency review remain later work.

PowerShell, from this directory:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
# Only with an explicitly selected connected test device/emulator:
.\gradlew.bat :app:connectedDebugAndroidTest
```

If Android Studio has not created local.properties, set `ANDROID_HOME` to your SDK directory for the current terminal or create an ignored local.properties with `sdk.dir=C:/path/to/Android/Sdk`. Never commit machine-specific SDK paths.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Tests: `app/build/reports/tests/testDebugUnitTest/index.html`; lint: `app/build/reports/lint-results-debug.html`; connected-test reports: `app/build/reports/androidTests/connected/debug/index.html` when run.

## Core and diagnostic boundary

- Optional Usage Access via Android Settings; AppOps rechecked on return and while foreground. Today/yesterday/last seven calendar days query raw daily Android foreground buckets. Returned bounds and unknown coverage are shown; no exact custom-range or screen-on totals are claimed. Package identifiers are used directly; no broad installed-app permission is requested.
- Optional reusable intentions can be saved, edited, loaded into the form and deleted with confirmation; saving does not start a session. Optional target is a package identifier, not inferred activity/purpose. Reminder preference is stored only; production session reminders are not integrated.
- Manual single-session start, pause/resume, extend five minutes, finish/cancel and reload now use Room. Transactions plus a unique nullable active-slot index protect one-open-session integrity. Title, original duration, optional target and preference are snapshots; later template edits do not alter recorded sessions. Events append atomically. Ended history remains when a new session starts; the diagnostic list shows the latest 100 ended records without deleting older ones.
- Optional reflection is stored separately: absent, explicitly skipped, completed, partial and not completed are distinct. Finishing never assigns an outcome. Only finished sessions accept reflection, which can be updated by explicit user action. No effectiveness metric is inferred.
- Upgrade imports the legacy `feasibility.db` record once into `focusloop-core.db`, retaining the original database unchanged as a recovery snapshot. An import marker prevents duplicates; failures are visible and retryable. No mixed-version writers run after app replacement. Downgrading to Phase 1 sees only that old snapshot, not subsequent Room history. Corruption does not trigger automatic database deletion; severely corrupt core data may require an explicit OS-level app-storage reset after reviewing recovery options.
- Confirmed history clearing deletes all core sessions/events/reflections and the preserved legacy record, retaining the import tombstone to prevent resurrection. Saved intentions and Android usage statistics remain. Deleting a saved intention does not change past sessions. All storage runs off the UI thread; storage failures label the last displayed record and disable mutations until recovery/retry.
- Active time excludes explicit pauses; elapsed includes pauses. Both include time outside FocusLoop while running as appropriate. Same-boot recovery uses monotonic anchors. After reboot or missing/changed boot identity, only last persisted known intervals remain; gap is marked unknown and user explicitly resumes or ends. Timer expiry does not force completion or infer success.
- Independent opt-in 60-second inexact notification probe with request, Settings, schedule, cancel, receiver-lag status. Granting permission alone does not schedule it. No exact alarm, persistent service, boot rescheduling, or production session checkpoints. Receiver delivery does not prove notification visibility.
- Compose Material 3, ViewModel, coroutines/StateFlow, pure Kotlin domain and Android adapters. Uses supplied palette and paired diagnostic panels at >=600 dp available width, falling back to a single column for narrow windows or large fonts. Actual physical fold/unfold and large-font checks must be reported separately. Full Canva screens and navigation remain deferred.
- No Internet permission, analytics SDK, accounts, cloud, AccessibilityService, interception or blocking. Usage records remain memory-only; Room contains app-owned intentions/sessions/events and explicitly self-reported reflections. Permission/probe metadata and both databases have backup/transfer exclusions. Actual OEM transfer behavior remains unverified.

See the [phase 0 audit](../docs/engineering/PHASE_0_AUDIT.md), [historical phase 1 results](../docs/engineering/PHASE_1_RESULTS.md), [current core/device results](../docs/engineering/PHASE_2_RESULTS.md) and [Fold5 checklist](../docs/qa/PHASE_1_FOLD5_CHECKLIST.md). Current work stops after the bounded core slice; full analytics, Canva screens and integration remain later phases.
